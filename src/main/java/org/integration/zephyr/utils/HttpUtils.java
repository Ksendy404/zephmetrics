package org.integration.zephyr.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.integration.zephyr.core.Config;
import org.integration.zephyr.enums.ConfigProperty;
import org.integration.zephyr.service.AuthService;

import java.io.IOException;

public class HttpUtils {

    public static CloseableHttpClient getHttpClient() {
        return HttpClientBuilder
                .create()
                .setDefaultCookieStore(AuthService.COOKIE)
                .build();
    }

    private static HttpResponse get(Config config, String url) throws IOException {
        CloseableHttpClient httpClient = getHttpClient();
        String uri = config.getValue(ConfigProperty.JIRA_URL) + url;
        Utils.log("GET: " + uri);
        HttpGet request = new HttpGet(uri);
        setCommonHeaders(request);
        return httpClient.execute(request);
    }

    private static void setCommonHeaders(HttpRequestBase request) throws IOException {
        request.setHeader("Accept", "application/json");
    }

    public static String getAndReturnBody(Config config, String url) throws IOException {
        HttpResponse response = get(config, url);
        return Utils.readInputStream(response.getEntity().getContent());
    }

    public static HttpResponse post(Config config, String url, Object entity) throws IOException {
        String json = ObjectTransformer.serialize(entity);

        CloseableHttpClient httpClient = getHttpClient();
        String uri = config.getValue(ConfigProperty.JIRA_URL) + url;
        Utils.log("POST: " + uri);
        HttpPost request = new HttpPost(uri);
        setCommonHeaders(request);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(json));
        return httpClient.execute(request);
    }

    public static HttpResponse put(Config config, String url, Object entity) throws IOException {
        String json = ObjectTransformer.serialize(entity);

        CloseableHttpClient httpClient = getHttpClient();
        String uri = config.getValue(ConfigProperty.JIRA_URL) + url;
        Utils.log("PUT: " + uri);
        HttpPut request = new HttpPut(uri);
        setCommonHeaders(request);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(json));
        CloseableHttpResponse response = httpClient.execute(request);
        httpClient.close();
        return response;
    }

    public static void ensureResponse(HttpResponse response, int expectedStatusCode, String failureMessage) {
        if (response.getStatusLine().getStatusCode() != expectedStatusCode) {
            String responseBody;
            try {
                responseBody = IOUtils.toString(response.getEntity().getContent());
            } catch (IOException e) {
                Utils.log("Failed to parse response", e);
                responseBody = "<no response>";
            }
            throw new ZephyrSyncException(failureMessage + ": " + responseBody);
        }
    }

}