package org.openl.rules.rest;

import org.openl.message.Severity;

public class MessageDescription {

    private final long id;
    private final String summary;
    private final Severity severity;
    private final String url;

    public MessageDescription(long id, String summary, Severity severity, String url) {
        this.id = id;
        this.summary = summary;
        this.severity = severity;
        this.url = url;
    }

    public long getId() {
        return id;
    }

    public String getSummary() {
        return summary;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getUrl() {
        return url;
    }

}
