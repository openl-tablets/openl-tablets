package org.openl.rules.ruleservice.servlet;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ServiceInfo {
    private final Date startedTime;
    private final String name;
    private final List<String> methodNames;
    private final ServiceResource[] serviceResources;

    public ServiceInfo(Date startedTime, String name, List<String> methodNames, String url, String urlName) {
        this(startedTime,
            name,
            methodNames,
            new ServiceResource[] { new ServiceResource(url, urlName) });
    }

    ServiceInfo(Date startedTime,
            String name,
            List<String> methodNames,
            ServiceResource[] serviceResources) {
        if (startedTime == null || name == null) {
            throw new IllegalArgumentException("'startedTime' and 'name' parameters must not be null!");
        }
        if (methodNames == null) {
            methodNames = Collections.emptyList();
        }
        this.startedTime = startedTime;
        this.name = name;
        this.methodNames = methodNames;
        this.serviceResources = serviceResources;
    }

    public String getName() {
        return name;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public List<String> getMethodNames() {
        return methodNames;
    }

    public ServiceResource[] getServiceResources() {
        return serviceResources;
    }
}
