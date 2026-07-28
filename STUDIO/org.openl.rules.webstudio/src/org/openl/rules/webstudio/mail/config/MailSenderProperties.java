package org.openl.rules.webstudio.mail.config;

import lombok.RequiredArgsConstructor;

import org.openl.util.StringUtils;

@RequiredArgsConstructor
public class MailSenderProperties {

    private final String url;
    private final String user;
    private final String password;

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
