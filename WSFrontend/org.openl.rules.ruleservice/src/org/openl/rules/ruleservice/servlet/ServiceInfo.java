package org.openl.rules.ruleservice.servlet;

import java.util.*;

public class ServiceInfo {
    private final Date startedTime;
    private final String name;
    private final List<String> methodNames;
    private final Map<String, String> urls = new HashMap<>(1);

    public ServiceInfo(Date startedTime, String name, List<String> methodNames, String url, String urlName) {
        if (startedTime == null || name == null) {
            throw new IllegalArgumentException("'startedTime' and 'name' parameters must not be null!");
        }
        if (methodNames == null) {
            methodNames = Collections.emptyList();
        }
        this.startedTime = startedTime;
        this.name = name;
        this.methodNames = methodNames;
        urls.put(urlName, url);
    }

    ServiceInfo(Date startedTime, String name, List<String> methodNames, Map<String, String> urls) {
        this.startedTime = startedTime;
        this.name = name;
        this.methodNames = methodNames;
        this.urls.putAll(urls);
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

    public Map<String, String> getUrls() {
        return urls;
    }
}
