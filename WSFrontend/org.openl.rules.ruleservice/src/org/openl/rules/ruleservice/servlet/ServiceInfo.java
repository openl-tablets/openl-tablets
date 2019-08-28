package org.openl.rules.ruleservice.servlet;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ServiceInfo {
    private final Date startedTime;
    private final String name;
    private final String servicePath;
    private final Map<String, String> urls = new HashMap<>(1);

    public ServiceInfo(Date startedTime, String name, String url, String urlName, String servicePath) {
        if (startedTime == null || name == null) {
            throw new IllegalArgumentException("'startedTime' and 'name' parameters must not be null!");
        }
        this.startedTime = startedTime;
        this.name = name;
        this.servicePath = servicePath;
        urls.put(urlName, url);
    }

    public ServiceInfo(Date startedTime, String name, Map<String, String> urls, String servicePath) {
        this.startedTime = startedTime;
        this.name = name;
        this.servicePath = servicePath;
        this.urls.putAll(urls);
    }

    public String getName() {
        return name;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public String getServicePath() {
        return servicePath;
    }
}
