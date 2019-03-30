package org.openl.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.openl.message.OpenLMessage;

/**
 * The <code>ValidationResult</code> defines contract that used in validation process.
 * 
 * While OpenL engine base concept is rules sets the validation process used list of {@link OpenlMessage} as a container
 * to accumulate problems of each rule.
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
    private Collection<OpenLMessage> messages;

    /**
     * Creates new instance of validation result.
     * 
     * @param status status value
     */
    public ValidationResult(ValidationStatus status) {
        this.status = status;
    }

    public ValidationResult(ValidationStatus status, Collection<OpenLMessage> messages) {
        this.status = status;
        if (messages == null) {
            this.messages = Collections.emptyList();
        } else {
            this.messages = new LinkedHashSet<>(messages);
        }
    }

    /**
     * Gets status of validation.
     * 
     * @return status value
     */
    public ValidationStatus getStatus() {
        return status;
    }

    /**
     * Gets validation messages.
     * 
     * @return list of messages
     */
    public Collection<OpenLMessage> getMessages() {
        if (messages == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(messages);
    }
}
