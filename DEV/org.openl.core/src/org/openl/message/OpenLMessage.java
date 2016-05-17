package org.openl.message;

import org.openl.util.StringUtils;

/**
 * The <code>OpenLMessage</code> class defines a message abstraction. Messages
 * used in the OpenL engine as warnings, errors or information statements to
 * ease communication between engine and end user.
 * 
 */
public class OpenLMessage {
    
    /**
     * Message's brief information.
     */
    private String summary;

    /**
     * Message's severity.
     */
    private Severity severity;

    /**
     * Constructs new instance of message with INFO severity.
     * 
     * @param summary brief information
     */
    public OpenLMessage(String summary) {
        this(summary, Severity.INFO);
    }

    /**
     * Constructs new instance of message.
     * 
     * @param summary brief information
     * @param severity message severity
     */
    public OpenLMessage(String summary, Severity severity) {
        this.summary = summary;
        this.severity = severity;
    }

    /**
     * Gets message summary.
     * 
     * @return message summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Gets message severity.
     * 
     * @return message severity
     */
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return summary == null ? StringUtils.EMPTY : summary;
    }

    public String getSourceLocation() {
        return null;
    }
}
