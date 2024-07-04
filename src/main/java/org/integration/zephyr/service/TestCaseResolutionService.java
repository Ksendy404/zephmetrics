package org.integration.zephyr.service;

import org.integration.zephyr.core.Config;
import org.integration.zephyr.beans.TestCase;
import org.integration.zephyr.transformer.ReportTransformer;
import org.integration.zephyr.transformer.ReportTransformerFactory;
import org.integration.zephyr.utils.ZephyrSyncException;

import java.util.Iterator;
import java.util.List;

import static org.integration.zephyr.enums.ConfigProperty.REPORT_PATH;
import static org.integration.zephyr.enums.ConfigProperty.REPORT_TYPE;

public class TestCaseResolutionService {

    private Config config;

    public TestCaseResolutionService(Config config) {
        this.config = config;
    }

    public List<TestCase> resolveTestCases() {
        String reportType = config.getValue(REPORT_TYPE);
        String path = config.getValue(REPORT_PATH);
        ReportTransformer transformer = ReportTransformerFactory.getInstance().getTransformer(reportType);
        List<TestCase> testCases = transformer.transformToTestCases(path);
        if (testCases == null) {
            throw new ZephyrSyncException("No Test Cases extracted from the Test Report");
        }
        for (Iterator<TestCase> it = testCases.iterator(); it.hasNext(); ) {
            TestCase testCase = it.next();
            if (testCase.getName() == null || testCase.getName().length() == 0) {
                it.remove();
            }
        }
        if (testCases.isEmpty()) {
            throw new ZephyrSyncException("No Test Cases extracted from the Test Report");
        }
        return testCases;
    }
}
