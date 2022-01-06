package org.openl.rules.rest.model;

import org.openl.rules.rest.validation.MailConfigConstraint;

@MailConfigConstraint
public class MailConfigModel {

    private String url;

    private String username;

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
