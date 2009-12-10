package org.openl;

public class OpenLMessage {
    
    public static final Severity SEVERITY_INFO = new Severity("info", 1);
    public static final Severity SEVERITY_WARN = new Severity("warn", 1);
    public static final Severity SEVERITY_ERROR = new Severity("error", 1);
    public static final Severity SEVERITY_FATAL = new Severity("fatal", 1);

    private String summary;
    private String details;
    private Severity severity;
    
    public OpenLMessage(String summary, String details) {
        this(summary, details, SEVERITY_INFO);
    }
    
    public OpenLMessage(String summary, String details, Severity severity) {
        this.summary = summary;
        this.details = details;
        this.severity = severity;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }

    public Severity getSeverity() {
        return severity;
    }
   
}
