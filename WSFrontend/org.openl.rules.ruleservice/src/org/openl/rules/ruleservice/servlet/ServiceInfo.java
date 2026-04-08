package org.openl.rules.ruleservice.servlet;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

public class ServiceInfo {

    @Getter
    private final Date startedTime;
    @Getter
    private final String name;
    @Getter
    private final String servicePath;
    @Getter
    private final Map<String, String> urls = new HashMap<>(1);
    private final boolean hasManifest;
    @Getter
    private final String deploymentName;
    @Getter
    private final ServiceStatus status;

    public enum ServiceStatus {
        DEPLOYED,
        FAILED
    }

    public ServiceInfo(Date startedTime,
                       String name,
                       boolean failed,
                       Map<String, String> urls,
                       String servicePath,
                       boolean hasManifest,
                       String deploymentName) {
        this.startedTime = Objects.requireNonNull(startedTime, "startedTime cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.status = failed ? ServiceStatus.FAILED : ServiceStatus.DEPLOYED;
        this.servicePath = servicePath;
        this.urls.putAll(urls);
        this.hasManifest = hasManifest;
        this.deploymentName = deploymentName;
    }

    public boolean getHasManifest() {
        return hasManifest;
    }
}
