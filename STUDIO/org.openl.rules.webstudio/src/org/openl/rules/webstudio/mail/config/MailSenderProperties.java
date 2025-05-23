package org.openl.rules.webstudio.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import org.openl.util.StringUtils;

@ConstructorBinding
@ConfigurationProperties(prefix = "mail")
public class MailSenderProperties {

    private final String url;
    private final String username;
    private final String password;

    public MailSenderProperties(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isValidEmailSettings() {
        return StringUtils.isNotBlank(url) && StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password);
    }
}
