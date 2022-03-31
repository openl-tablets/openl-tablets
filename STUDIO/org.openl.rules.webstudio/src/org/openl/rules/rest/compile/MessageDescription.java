package org.openl.rules.rest.compile;

import org.openl.message.Severity;

public class MessageDescription {

    private final long id;
    private final String summary;
    private final Severity severity;

    public MessageDescription(long id, String summary, Severity severity) {
        this.id = id;
        this.summary = summary;
        this.severity = severity;
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

}
