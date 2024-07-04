package org.integration.zephyr.core;

import org.integration.zephyr.enums.ConfigProperty;
import org.integration.zephyr.transformer.ReportTransformerFactory;

import java.util.List;

public class AutodetectReportType implements Config.Loader {
    public void execute(Config config) {
        List<String> supportedReportTransformers = ReportTransformerFactory.getInstance().getSupportedReportTransformers();
        if (supportedReportTransformers.size() == 1) {
            config.applyDefault(ConfigProperty.REPORT_TYPE, supportedReportTransformers.get(0));
        }
    }
}
