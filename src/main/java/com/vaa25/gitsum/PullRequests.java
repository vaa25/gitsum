package com.vaa25.gitsum;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Created by vaa25 on 05.09.16.
 */
public final class PullRequests {
    private final PullRequestService pullRequestService;
    private final IssueService issueService;
    private final IRepositoryIdProvider provider;
    private final Common common;
    private final String user;
    private final LocalDate since;

    public PullRequests(GitHubClient client, final IRepositoryIdProvider provider, final String user, final Month since) {
        pullRequestService = new PullRequestService(client);
        this.provider = provider;
        common = new Common();
        issueService = new IssueService(client);
        this.user = user;
        this.since = LocalDate.now().withMonth(since.getValue()).withDayOfMonth(1);
    }

    public Map<PullRequest, Duration> run() throws IOException {
        final List<PullRequest> pullRequests = pullrequestsFromGithub();
        final Map<PullRequest, Duration> durationByPullrequest = durationByPullrequest(pullRequests);

        return durationByPullrequest;
    }

    public Map<LocalDate, Duration> durationByDate(final Map<PullRequest, Duration> durationByPullrequest) {
        Map<LocalDate, Duration> durationByDate = durationByPullrequest.entrySet()
            .stream()
            .collect(toMap((entry) -> pullrequestDate(entry.getKey()), Map.Entry::getValue, Duration::plus, TreeMap::new));
        return durationByDate;
    }

    private List<Comment> comments(PullRequest pullRequest) {
        try {
            return issueService.getComments(provider, pullRequest.getNumber());
        } catch (IOException e) {
            throw new RuntimeException(pullRequest.toString());
        }
    }

    private List<PullRequest> pullrequestsFromGithub() throws IOException {
        return pullRequestService.getPullRequests(provider, "closed")
            .stream()
            .filter(pullRequest -> pullrequestDate(pullRequest).compareTo(since) >= 0)
            .collect(toList());
    }

    private Map<PullRequest, Duration> durationByPullrequest(final List<PullRequest> pullRequests) {
        return pullRequests.stream()
            .collect(groupingBy(pull -> pull, pullrequestTreeMap(), mapping(pr -> {
                try {
                    final List<Comment> comments = comments(pr);
                    final Duration duration = comments.stream().filter(comment -> {
                        final String body = comment.getBody();
                        final String login = comment.getUser().getLogin();
                        return user.equals(login) && common.hasCash(body);
                    }).map(comment -> common.duration(comment.getBody())).reduce(Duration::plus).orElse(Duration.ZERO);
                    return duration;
                } catch (Exception e) {
                    System.err.println(pr.getClosedAt().toString() + '\t' + pr.toString() + ":\t" + e.getMessage());
                    return Duration.ZERO;
                }
            }, reducing(Duration.ZERO, Duration::plus))))
            .entrySet()
            .stream()
            .filter(entry -> !entry.getValue().equals(Duration.ZERO))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, Duration::plus, pullrequestTreeMap()));
    }

    private Supplier<TreeMap<PullRequest, Duration>> pullrequestTreeMap() {
        return () -> new TreeMap<>((pr1, pr2) -> pr1.getClosedAt().compareTo(pr2.getClosedAt()));
    }


    public void printPullrequests(final Map<PullRequest, Duration> durationByDate) {
        for (Map.Entry<PullRequest, Duration> entry : durationByDate.entrySet()) {
            final long minutes = entry.getValue().toMinutes();
            final long hours = minutes / 60;
            final PullRequest pr = entry.getKey();
            System.out.println(pullrequestDate(pr).toString() + '\t' + pr.toString() + '\t' + hours + "h " + (minutes - hours * 60) + 'm');
        }
    }


    private LocalDate pullrequestDate(final PullRequest issue) {
        return issue.getClosedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
