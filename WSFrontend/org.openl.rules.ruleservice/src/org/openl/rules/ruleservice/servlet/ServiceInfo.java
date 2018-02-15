package org.openl.rules.ruleservice.servlet;

import java.util.Date;

public class ServiceInfo {
    private final Date startedTime;
    private final String name;
    private final ServiceResource[] serviceResources;

    public ServiceInfo(Date startedTime, String name, String url, String urlName) {
        this(startedTime, name, new ServiceResource[] { new ServiceResource(url, urlName) });
    }

    public ServiceInfo(Date startedTime, String name, ServiceResource[] serviceResources) {
        if (startedTime == null || name == null) {
            throw new IllegalArgumentException("'startedTime' and 'name' parameters must not be null!");
        }
        this.startedTime = startedTime;
        this.name = name;
        this.serviceResources = serviceResources;
    }

    public String getName() {
        return name;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public ServiceResource[] getServiceResources() {
        return serviceResources;
    }
}
