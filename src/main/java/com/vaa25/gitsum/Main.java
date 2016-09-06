package com.vaa25.gitsum;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.SubRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.LittleEndianOutput;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

/**
 * Created by vaa25 on 04.09.16.
 */
public final class Main {
    private final IRepositoryIdProvider provider = new RepositoryId("vkuchyn", "socialscore");
    private final Common common = new Common();
    private final String user = "vaa25";
    private final Month since = Month.JUNE;

    public static void main(String[] args) throws IOException {
        new Main().run();
    }

    private void run() throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token("******************");
        Issues issues = new Issues(client, provider, user, since, asList(479, 634));
        PullRequests pullRequests = new PullRequests(client, provider, user, since);

        final Map<PullRequest, Duration> durationByPullrequest = pullRequests.run();
        System.out.println("======= Суммарно по пуллреквестам ========");
        pullRequests.printPullrequests(durationByPullrequest);
        final Map<LocalDate, Duration> pr = pullRequests.durationByDate(durationByPullrequest);


        final Map<Issue, Duration> durationByIssue = issues.run();
        System.out.println("======= Суммарно по таскам ========");
        issues.printIssues(durationByIssue);
        final Map<LocalDate, Duration> issue = issues.durationByDate(durationByIssue);

        pr.entrySet().forEach(entry -> issue.merge(entry.getKey(), entry.getValue(), Duration::plus));

        System.out.println("======== Суммарно по дням ========");
        common.print(issue);

        final Map<Month, Duration> durationByMonth = issue.entrySet()
            .stream()
            .collect(toMap((entry) -> entry.getKey().getMonth(), Map.Entry::getValue, Duration::plus, TreeMap::new));

        System.out.println("======== Суммарно по месяцам ========");
        common.print(durationByMonth);

//        Xlsx xlsx = new Xlsx();
//        xlsx.createDays(issue);
//        xlsx.createUser(user, durationByPullrequest, durationByIssue, issue);
//        xlsx.saveWorkbook();

    }



}
