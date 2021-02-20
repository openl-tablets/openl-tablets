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
    private final boolean hasManifest;
    private final String deploymentName;

    private enum ServiceStatus {
        DEPLOYED,
        FAILED
    }

    public ServiceInfo(Date startedTime,
            String name,
            Map<String, String> urls,
            String servicePath,
            boolean hasManifest,
            String deploymentName) {
        this.startedTime = Objects.requireNonNull(startedTime, "startedTime cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.servicePath = servicePath;
        this.urls.putAll(urls);
        this.hasManifest = hasManifest;
        this.deploymentName = deploymentName;
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
        return urls.isEmpty() ? ServiceStatus.FAILED : ServiceStatus.DEPLOYED;
    }

    public boolean getHasManifest() {
        return hasManifest;
    }

    public String getDeploymentName() {
        return deploymentName;
    }
}
