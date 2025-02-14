package org.openl.rules.webstudio.mail.config;

import org.openl.util.StringUtils;

public class MailSenderProperties {

    private final String url;
    private final String user;
    private final String password;

    public MailSenderProperties(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean isValidEmailSettings() {
        return StringUtils.isNotBlank(url) && StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password);
    }
}
