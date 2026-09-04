package org.openl.rules.webstudio.mail.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.util.StringUtils;

@RequiredArgsConstructor
public class MailSenderProperties {

    @Getter
    private final String url;
    @Getter
    private final String user;
    @Getter
    private final String password;

    public boolean isValidEmailSettings() {
        return StringUtils.isNotBlank(url) && StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password);
    }
}
