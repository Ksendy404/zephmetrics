package org.integration.zephyr.service;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicCookieStore;
import org.integration.zephyr.beans.jira.ErrorResponse;
import org.integration.zephyr.beans.jira.Login;
import org.integration.zephyr.beans.jira.SessionResponse;
import org.integration.zephyr.core.Config;
import org.integration.zephyr.enums.ConfigProperty;
import org.integration.zephyr.utils.HttpUtils;
import org.integration.zephyr.utils.ObjectTransformer;
import org.integration.zephyr.utils.Utils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.integration.zephyr.utils.Utils.log;

public class AuthService {

    public static final BasicCookieStore COOKIE = new BasicCookieStore();
    private static String jSessionId;

    private Config config;

    public AuthService(Config config) {
        this.config = config;
    }

    public void authenticateInJira() throws IOException {
        if (jSessionId == null) {
            Login login = new Login(config.getValue(ConfigProperty.USERNAME), config.getValue(ConfigProperty.PASSWORD));

            HttpResponse response = HttpUtils.post(config, "auth/1/session", login);
            if (response.getStatusLine().getStatusCode() == 403) {
                if (response.containsHeader("X-Authentication-Denied-Reason")) {
                    log("ERROR: JIRA authentication denied reason: " + response.getFirstHeader("X-Authentication-Denied-Reason").getValue());
                }

                ErrorResponse errorResponse = ObjectTransformer.deserialize(Utils.readInputStream(response.getEntity().getContent()), ErrorResponse.class);
                List<String> errorMessages = errorResponse.getErrorMessages();
                log("ERROR: JIRA authentication failed, error messages: " + errorMessages.stream().collect(Collectors.joining(", ")));
                return;
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                log("ERROR: JIRA authentication failed: " + response.getStatusLine().getProtocolVersion() + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                return;
            }
            SessionResponse loginResponse = ObjectTransformer.deserialize(Utils.readInputStream(response.getEntity().getContent()), SessionResponse.class);
            if (loginResponse != null) {
                jSessionId = loginResponse.getSession().get("value");
            }
        }
    }

}