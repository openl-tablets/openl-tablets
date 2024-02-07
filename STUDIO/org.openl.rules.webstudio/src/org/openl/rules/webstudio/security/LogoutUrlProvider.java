package org.openl.rules.webstudio.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class LogoutUrlProvider {
    private final String logoutUrl;
    private String logoutUrlParameters;

    public LogoutUrlProvider(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    @Autowired(required = false)
    @Qualifier("logoutUrlParameters")
    public void setLogoutUrlParameters(String logoutUrlParameters) {
        this.logoutUrlParameters = logoutUrlParameters;
    }

    public String getUrl() {
        return logoutUrl + (logoutUrlParameters != null ? logoutUrlParameters : "");
    }
}
