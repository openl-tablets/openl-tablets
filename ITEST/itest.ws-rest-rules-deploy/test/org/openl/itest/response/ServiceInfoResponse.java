package org.openl.itest.response;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ServiceInfoResponse  {

    private String name;
    private Map<String, String> urls = new HashMap<>();
    private Date startedTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public void setUrls(Map<String, String> urls) {
        this.urls = urls;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(Date startedTime) {
        this.startedTime = startedTime;
    }
}
