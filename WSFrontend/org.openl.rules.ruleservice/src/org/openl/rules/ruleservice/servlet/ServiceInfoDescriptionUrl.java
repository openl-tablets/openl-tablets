package org.openl.rules.ruleservice.servlet;

public class ServiceInfoDescriptionUrl {
    private final String url;
    private final String description;

    public ServiceInfoDescriptionUrl(String url, String description) {
        this.url = url;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }
}
