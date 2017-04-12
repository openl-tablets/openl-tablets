package org.openl.rules.ruleservice.servlet;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ServiceInfo {
    private final Date startedTime;
    private final String name;
    private final List<String> methodNames;
    private final String url;
    private final String address;
    private final String urlDescription;

    public ServiceInfo(Date startedTime, String name, List<String> methodNames, String url, String urlDescription) {
        this(startedTime, name, methodNames, url, urlDescription, null);
    }
    
    public ServiceInfo(Date startedTime, String name, List<String> methodNames, String url, String urlDescription, String address) {
        if (startedTime == null || name == null) {
            throw new IllegalArgumentException("'startedTime' and 'name' parameters must not be null!");
        }
        if (methodNames == null) {
            methodNames = Collections.emptyList();
        }
        this.startedTime = startedTime;
        this.name = name;
        this.methodNames = methodNames;
        this.url = url;
        this.urlDescription = urlDescription;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public String getUrl() {
        return url;
    }

    public String getUrlDescription() {
        return urlDescription;
    }

    public List<String> getMethodNames() {
        return methodNames;
    }
    
    public String getAddress() {
        return address;
    }
}
