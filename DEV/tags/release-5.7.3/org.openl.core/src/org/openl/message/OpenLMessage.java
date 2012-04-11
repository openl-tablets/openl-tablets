package org.openl.message;

import org.apache.commons.lang.StringUtils;

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
     * Message's detailed information.
     */
    private String details;

    /**
     * Message's severity.
     */
    private Severity severity;

    /**
     * Constructs new instance of message with INFO severity.
     * 
     * @param summary brief information
     * @param details detailed information
     */
    public OpenLMessage(String summary, String details) {
        this(summary, details, Severity.INFO);
    }

    /**
     * Constructs new instance of message.
     * 
     * @param summary brief information
     * @param details detailed information
     * @param severity message severity
     */
    public OpenLMessage(String summary, String details, Severity severity) {
        this.summary = summary;
        this.details = details;
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
     * Gets message details.
     * 
     * @return message details
     */
    public String getDetails() {
        return details;
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
        return StringUtils.defaultString(summary);
    }

}
