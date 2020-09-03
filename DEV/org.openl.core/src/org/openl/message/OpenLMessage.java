package org.openl.message;

import java.util.concurrent.atomic.AtomicInteger;

import org.openl.util.StringUtils;

/**
 * The <code>OpenLMessage</code> class defines a message abstraction. Messages used in the OpenL engine as warnings,
 * errors or information statements to ease communication between engine and end user.
 */
public class OpenLMessage {

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private final int id = idCounter.incrementAndGet();

    /**
     * Message's brief information.
     */
    private final String summary;

    /**
     * Message's severity.
     */
    private final Severity severity;

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

    public boolean isError() {
        return Severity.ERROR.equals(getSeverity());
    }

    public boolean isWarn() {
        return Severity.WARN.equals(getSeverity());
    }

    public boolean isFatal() {
        return Severity.FATAL.equals(getSeverity());
    }

    public boolean isInfo() {
        return Severity.INFO.equals(getSeverity());
    }

    @Override
    public String toString() {
        return summary == null ? StringUtils.EMPTY : summary;
    }

    public String getSourceLocation() {
        return null;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (severity == null ? 0 : severity.hashCode());
        result = prime * result + (summary == null ? 0 : summary.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OpenLMessage other = (OpenLMessage) obj;
        if (severity != other.severity) {
            return false;
        }
        if (summary == null) {
            if (other.summary != null) {
                return false;
            }
        } else if (!summary.equals(other.summary)) {
            return false;
        }
        return true;
    }

}
