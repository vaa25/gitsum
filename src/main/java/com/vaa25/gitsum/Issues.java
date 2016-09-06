package com.vaa25.gitsum;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Created by vaa25 on 05.09.16.
 */
public final class Issues {

    private final IssueService issueService;
    private final IRepositoryIdProvider provider;
    private final Common common;
    private final String user;
    private final LocalDate since;
    private final List<Integer> ignore;

    public Issues(GitHubClient client, final IRepositoryIdProvider provider, final String user, final Month since, final List<Integer> ignore) {
        issueService = new IssueService(client);
        this.provider = provider;
        this.user = user;
        this.ignore = ignore;
        common = new Common();
        this.since = LocalDate.now().withMonth(since.getValue()).withDayOfMonth(1);
    }

    public Map<Issue, Duration> run() throws IOException {
        final List<Issue> issues = issuesFromGithub();
        final Map<Issue, Duration> durationByIssue = durationByIssue(issues);
        System.out.println("======= Суммарно по таскам ========");
        printIssues(durationByIssue);
        return durationByIssue;
    }

    public Map<LocalDate, Duration> durationByDate(Map<Issue, Duration> durationByIssue){
        Map<LocalDate, Duration> durationByDate = durationByIssue.entrySet()
            .stream()
            .collect(toMap((entry) -> issuesDate(entry.getKey()), Map.Entry::getValue, Duration::plus, TreeMap::new));
        return durationByDate;
    }

    private List<Issue> issuesFromGithub() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("state", "closed");
        params.put("filter", "assigned");
        params.put("since", since.atTime(0, 0).toString());


        return issueService.getIssues(provider, params)
            .stream()
            .filter(issue -> (!issue.getHtmlUrl().contains("/pull/")) || issue.getBody()
                .trim()
                .toLowerCase()
                .contains("Team meeting"))
            .filter(issue -> issuesMonth(issue).compareTo(since.getMonth()) >= 0)
            .filter(issue -> !ignore.contains(issue.getNumber()))
            .collect(toList());
    }

    private Map<Issue, Duration> durationByIssue(final List<Issue> issues) {
        return issues.stream()
            .collect(groupingBy(issue -> issue, issueTreeMap(), mapping(issue -> {
                try {
                    final List<Comment> comments = comments(issue);
                    final Duration duration = comments.stream().filter(comment -> {
                        final String body = comment.getBody();
                        final String login = comment.getUser().getLogin();
                        return user.equals(login) && common.hasCash(body);
                    }).map(comment -> common.duration(comment.getBody())).reduce(Duration::plus).orElse(Duration.ZERO);
                    return duration;
                } catch (Exception e) {
                    System.err.println(issue.getClosedAt()
                        .toString() + '\t' + issue.toString() + ":\t" + e.getMessage());
                    return Duration.ZERO;
                }
            }, reducing(Duration.ZERO, Duration::plus))))
            .entrySet()
            .stream()
            .filter(entry -> !entry.getValue().equals(Duration.ZERO))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, Duration::plus, issueTreeMap()));
    }

    private Supplier<TreeMap<Issue, Duration>> issueTreeMap() {
        return () -> new TreeMap<>((issue1, issue2) -> issue1.getClosedAt().compareTo(issue2.getClosedAt()));
    }


    public void printIssues(final Map<Issue, Duration> durationByDate) {
        for (Map.Entry<Issue, Duration> entry : durationByDate.entrySet()) {
            final long minutes = entry.getValue().toMinutes();
            final long hours = minutes / 60;
            final Issue issue = entry.getKey();
            System.out.println(issuesDate(issue).toString() + '\t' + issue.toString() + '\t' + hours + "h " + (minutes - hours * 60) + 'm');
        }
    }


    private Month issuesMonth(final Issue issue) {
        return issue.getClosedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getMonth();
    }

    private LocalDate issuesDate(final Issue issue) {
        return issue.getClosedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private List<Comment> comments(Issue issue) {
        try {
            return issueService.getComments(provider, issue.getNumber());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


}
