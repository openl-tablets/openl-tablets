package org.openl.validation;

import java.util.ArrayList;
import java.util.List;

import org.openl.message.OpenLMessage;

/**
 * The <code>ValidationResult</code> defines contract that used in validation
 * process.
 * 
 * While OpenL engine base concept is rules sets the validation process used
 * list of {@link OpenlMessage} as a container to accumulate problems of each
 * rule.
 * 
 */
public class ValidationResult {

    /**
     * Validation status.
     */
    private ValidationStatus status;

    /**
     * Messages of validation process.
     */
    private List<OpenLMessage> messages;

    /**
     * Creates new instance of validation result.
     * 
     * @param status status value
     */
    public ValidationResult(ValidationStatus status) {
        this(status, null);
    }
    
    /**
     * Creates new instance of validation result.
     * 
     * @param status status value
     * @param messages list of validation messages
     */
    public ValidationResult(ValidationStatus status, List<OpenLMessage> messages) {
        this.status = status;
        this.messages = messages;
    }

    /**
     * Gets status of validation.
     * 
     * @return status value
     */
    public ValidationStatus getStatus() {
        return status;
    }

    public boolean hasMessages() {
        return !(messages == null || messages.isEmpty());
    }

    /**
     * Gets validation messages.
     * 
     * @return list of messages
     */
    public List<OpenLMessage> getMessages() {

        if (messages == null) {
            messages = new ArrayList<OpenLMessage>();
        }

        return messages;
    }
}
