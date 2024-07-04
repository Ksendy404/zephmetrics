package org.integration.zephyr.service;

import org.apache.http.HttpResponse;
import org.integration.zephyr.beans.jira.Metafield;
import org.integration.zephyr.beans.jira.Project;
import org.integration.zephyr.beans.zapi.Cycle;
import org.integration.zephyr.beans.zapi.CycleList;
import org.integration.zephyr.beans.zapi.NewCycle;
import org.integration.zephyr.core.Config;
import org.integration.zephyr.enums.ConfigProperty;
import org.integration.zephyr.utils.ObjectTransformer;
import org.integration.zephyr.utils.Utils;
import org.integration.zephyr.utils.ZephyrSyncException;

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;
import static org.integration.zephyr.utils.HttpUtils.getAndReturnBody;
import static org.integration.zephyr.utils.HttpUtils.post;

public class MetaInfoRetrievalService {

    private Config config;

    public MetaInfoRetrievalService(Config config) {
        this.config = config;
    }

    public MetaInfo retrieve() throws IOException {
        MetaInfo metaInfo = new MetaInfo();
        retrieveProjectMetaInfo(metaInfo);
        retrieveTestCycleId(metaInfo);
        return metaInfo;
    }

    private void retrieveProjectMetaInfo(MetaInfo metaInfo) throws IOException {
        String projectKey = config.getValue(ConfigProperty.PROJECT_KEY);
        String response = getAndReturnBody(config, format("api/2/project/%s", projectKey));
        Project project = ObjectTransformer.deserialize(response, Project.class);

        if (project == null || project.getKey() == null || !project.getKey().equals(projectKey)) {
            throw new ZephyrSyncException("Improper JIRA project retrieved");
        }

        String projectId = project.getId();
        Utils.log("Retrieved project ID - " + projectId);
        metaInfo.setProjectId(projectId);

        for (Metafield version : project.getVersions()) {
            if (version.getName().equals(config.getValue(ConfigProperty.RELEASE_VERSION))) {
                String versionId = version.getId();
                Utils.log("Retrieved version ID - " + versionId);
                metaInfo.setVersionId(versionId);
            }
        }
    }

    private void retrieveTestCycleId(MetaInfo metaInfo) throws IOException {
        String projectId = metaInfo.getProjectId();
        String versionId = metaInfo.getVersionId();
        if (projectId == null || versionId == null)
            throw new ZephyrSyncException("JIRA projectID or versionID are missing");

        String response = getAndReturnBody(config, format("zapi/latest/cycle?projectId=%s&versionId=%s", projectId, versionId));
        CycleList cycleList = ObjectTransformer.deserialize(response, CycleList.class);
        if (cycleList == null || cycleList.getCycleMap().isEmpty()) {
            throw new ZephyrSyncException("Unable to retrieve JIRA test cycle");
        }

        for (Map.Entry<String, Cycle> entry : cycleList.getCycleMap().entrySet()) {
            Cycle value = entry.getValue();
            if (value != null
                    && value.getProjectKey().equals(config.getValue(ConfigProperty.PROJECT_KEY))
                    && value.getVersionId().toString().equals(versionId)
                    && value.getName().equals(config.getValue(ConfigProperty.TEST_CYCLE))) {
                String cycleId = entry.getKey();
                Utils.log("Retrieved target Test Cycle ID - " + cycleId + "\n");
                metaInfo.setCycleId(cycleId);
                return;
            }
        }
        if (Boolean.parseBoolean(config.getValue(ConfigProperty.AUTO_CREATE_TEST_CYCLE))) {
            Utils.log("Creating new test cycle");
            String cycleId = createNewTestCycle(projectId, versionId);
            Utils.log("New test cycle created - " + cycleId);
            metaInfo.setCycleId(cycleId);
        } else {
            throw new ZephyrSyncException("Unable to retrieve JIRA test cycle");
        }
    }

    private String createNewTestCycle(String projectId, String versionId) throws IOException {
        NewCycle newCycle = new NewCycle();
        newCycle.setProjectId(projectId);
        newCycle.setVersionId(versionId);
        newCycle.setName(config.getValue(ConfigProperty.TEST_CYCLE));
        HttpResponse response = post(config, "zapi/latest/cycle", newCycle);
        Cycle cycle = ObjectTransformer.deserialize(Utils.readInputStream(response.getEntity().getContent()), Cycle.class);
        return cycle.getId();
    }

}
