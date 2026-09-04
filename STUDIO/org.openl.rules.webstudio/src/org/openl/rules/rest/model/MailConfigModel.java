package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

import org.openl.rules.rest.validation.MailConfigConstraint;

@MailConfigConstraint
@Deprecated(forRemoval = true)
public class MailConfigModel {

    @Getter
    @Parameter(description = "Mail server url", example = "smtps://mail.example.com:1587")
    private String url;

    @Getter
    @Parameter(description = "Username for authentication on mail server", example = "jhon@mail.example.com")
    private String username;

    @Getter
    @Parameter(description = "Password for authentication on mail server", example = "qwerty")
    private String password;

    public MailConfigModel setPassword(String password) {
        this.password = password;
        return this;
    }

    public MailConfigModel setUrl(String url) {
        this.url = url;
        return this;
    }

    public MailConfigModel setUsername(String username) {
        this.username = username;
        return this;
    }
}
