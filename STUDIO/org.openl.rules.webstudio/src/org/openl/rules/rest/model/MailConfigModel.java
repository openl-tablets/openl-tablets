package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.rules.rest.validation.MailConfigConstraint;

@MailConfigConstraint
@Deprecated(forRemoval = true)
public class MailConfigModel {

    @Parameter(description = "Mail server url", example = "smtps://mail.example.com:1587")
    private String url;

    @Parameter(description = "Username for authentication on mail server", example = "jhon@mail.example.com")
    private String username;

    @Parameter(description = "Password for authentication on mail server", example = "qwerty")
    private String password;

    public MailConfigModel() {
    }

    public MailConfigModel(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public MailConfigModel setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public MailConfigModel setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public MailConfigModel setUsername(String username) {
        this.username = username;
        return this;
    }
}
