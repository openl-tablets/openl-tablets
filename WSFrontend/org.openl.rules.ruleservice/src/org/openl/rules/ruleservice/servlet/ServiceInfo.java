package org.openl.rules.ruleservice.servlet;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceInfo {
    private final Date startedTime;
    private final String name;
    private final String servicePath;
    private final Map<String, String> urls = new HashMap<>(1);
    private final ServiceStatus status;

    private enum ServiceStatus {
        DEPLOYED,
        FAILED
    }

    public ServiceInfo(Date startedTime, String name, String servicePath) {
        this.startedTime = Objects.requireNonNull(startedTime, "startedTime cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.servicePath = servicePath;
        //if there are no urls, status - FAILED
        this.status = ServiceStatus.FAILED;
    }

    public ServiceInfo(Date startedTime, String name, String url, String urlName, String servicePath) {
        this.startedTime = Objects.requireNonNull(startedTime, "startedTime cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.servicePath = servicePath;
        urls.put(urlName, url);
        this.status = ServiceStatus.DEPLOYED;
    }

    public ServiceInfo(Date startedTime, String name, Map<String, String> urls, String servicePath) {
        this.startedTime = Objects.requireNonNull(startedTime, "startedTime cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.servicePath = servicePath;
        this.urls.putAll(urls);
        this.status = ServiceStatus.DEPLOYED;
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

    public ServiceStatus getStatus() {
        return status;
    }
}
