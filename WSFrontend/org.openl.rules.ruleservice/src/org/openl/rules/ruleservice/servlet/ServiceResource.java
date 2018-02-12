package org.openl.rules.ruleservice.servlet;

public class ServiceResource {
    private final String url;
    private final String name;

    public ServiceResource(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
