package org.integration.zephyr.transformer;

import org.integration.zephyr.beans.TestCase;

import java.util.List;

public interface ReportTransformer {

    String getType();

    List<TestCase> transformToTestCases(String reportPath);

}
