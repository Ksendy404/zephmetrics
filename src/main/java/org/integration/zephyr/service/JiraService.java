package org.integration.zephyr.service;

import org.apache.http.HttpResponse;
import org.integration.zephyr.beans.TestCase;
import org.integration.zephyr.beans.jira.*;
import org.integration.zephyr.core.Config;
import org.integration.zephyr.transformer.TestCaseToIssueTransformer;
import org.integration.zephyr.utils.ObjectTransformer;
import org.integration.zephyr.utils.ZephyrSyncException;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.integration.zephyr.enums.ConfigProperty.*;
import static org.integration.zephyr.utils.HttpUtils.*;
import static org.integration.zephyr.utils.Utils.log;
import static org.integration.zephyr.utils.Utils.readInputStream;

public class JiraService {

    private static final int TOP = 500;

    private Config config;

    public JiraService(Config config) {
        this.config = config;
    }

    public List<Issue> getTestIssues() throws IOException {
        int skip = 0;
        log("Fetching JIRA Test issues for the project");
        String search = "project='" + config.getValue(PROJECT_KEY) + "'%20and%20issueType=Test";
        SearchResponse searchResults = searchInJQL(search, skip);
        if (searchResults == null || searchResults.getIssues() == null) {
            throw new ZephyrSyncException("Unable to fetch JIRA test issues");
        }

        List<Issue> issues = searchResults.getIssues();

        int totalCount = searchResults.getTotal();
        if (totalCount > TOP) {
            while (issues.size() >= totalCount) {
                skip += TOP;
                SearchResponse newSearchResponse = searchInJQL(search, skip);
                totalCount = newSearchResponse.getTotal();
                if (issues.size() > totalCount) {
                    return getTestIssues();
                }
                issues.addAll(newSearchResponse.getIssues());
            }
        }
        log(format("Retrieved %s Test issues\n", issues.size()));
        return issues;
    }

    SearchResponse searchInJQL(String search, int skip) throws IOException {
        String response = getAndReturnBody(config, "api/2/search?jql=" + search + "&maxResults=" + TOP + "&startAt=" + skip);
        return ObjectTransformer.deserialize(response, SearchResponse.class);
    }

    public void createTestIssue(TestCase testCase) throws IOException {
        log("INFO: Creating JIRA Test item with Name: \"" + testCase.getName() + "\".");
        Issue issue = TestCaseToIssueTransformer.transform(config, testCase);

        HttpResponse response = post(config, "api/2/issue", issue);
        ensureResponse(response, 201, "ERROR: Could not create JIRA Test item");

        String responseBody = readInputStream(response.getEntity().getContent());
        Metafield result = ObjectTransformer.deserialize(responseBody, Metafield.class);
        if (result != null) {
            testCase.setId(Integer.valueOf(result.getId()));
            testCase.setKey(result.getKey());
        }
        log("INFO: Created. JIRA Test item Id is: [" + testCase.getKey() + "].");
    }

    public void linkToStory(TestCase testCase) throws IOException {
        List<String> storyKeys = testCase.getStoryKeys();
        if (Boolean.valueOf(config.getValue(FORCE_STORY_LINK))) {
            if (storyKeys == null || storyKeys.isEmpty()) {
                throw new ZephyrSyncException("Linking Test issues to Story is mandatory, please check if Story marker exists in " + testCase.getKey());
            }
        }
        if (storyKeys == null) return;

        log("Linking Test issue " + testCase.getKey() + " to Stories " + testCase.getStoryKeys());
        for (String storyKey : storyKeys) {
            HttpResponse response = post(config, "api/2/issueLink", createIssueLink(testCase, storyKey));
            ensureResponse(response, 201, "Could not link Test issue: " + testCase.getId() + " to Story " + storyKey + ". " +
                    "Please check if Story issue exists and is valid");
        }
    }

    private IssueLink createIssueLink(TestCase testCase, String storyKey) {
        IssueLinkDirection direction = IssueLinkDirection.ofValue(config.getValue(LINK_DIRECTION));
        if (direction == IssueLinkDirection.inward) {
            return new IssueLink(testCase.getKey(), storyKey.toUpperCase(), config.getValue(LINK_TYPE));
        }
        return new IssueLink(storyKey.toUpperCase(), testCase.getKey(), config.getValue(LINK_TYPE));
    }

}
