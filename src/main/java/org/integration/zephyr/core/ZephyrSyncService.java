package org.integration.zephyr.core;


import org.integration.zephyr.beans.TestCase;
import org.integration.zephyr.beans.jira.Issue;
import org.integration.zephyr.service.*;
import org.integration.zephyr.utils.CustomPropertyNamingStrategy;
import org.integration.zephyr.utils.ObjectTransformer;

import java.io.IOException;
import java.util.List;

public class ZephyrSyncService {

    private AuthService authService;
    private MetaInfoRetrievalService metaInfoRetrievalService;
    private TestCaseResolutionService testCaseResolutionService;
    private JiraService jiraService;
    private ZephyrService zephyrService;

    public ZephyrSyncService(Config config) {
        ObjectTransformer.setPropertyNamingStrategy(new CustomPropertyNamingStrategy(config));

        authService = new AuthService(config);
        metaInfoRetrievalService = new MetaInfoRetrievalService(config);
        testCaseResolutionService = new TestCaseResolutionService(config);
        jiraService = new JiraService(config);
        zephyrService = new ZephyrService(config);
    }

    public void execute() throws IOException, InterruptedException {
        authService.authenticateInJira();

        MetaInfo metaInfo = metaInfoRetrievalService.retrieve();

        List<TestCase> testCases = testCaseResolutionService.resolveTestCases();
        List<Issue> issues = jiraService.getTestIssues();

        zephyrService.mapTestCasesToIssues(testCases, issues);

        for (TestCase testCase : testCases) {
            if (testCase.getId() == null) {
                jiraService.createTestIssue(testCase);
                zephyrService.addStepsToTestIssue(testCase);
                jiraService.linkToStory(testCase);
            }
        }

        zephyrService.linkExecutionsToTestCycle(metaInfo, testCases);
        zephyrService.updateExecutionStatuses(testCases);

    }
}
