package org.openl.validation;

import org.openl.message.IOpenLMessages;
import org.openl.message.OpenLMessages;

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
    private IOpenLMessages messages;

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
    public ValidationResult(ValidationStatus status, IOpenLMessages messages) {
        this.status = status;
        if (messages == null) {
            this.messages = new OpenLMessages();
        } else {
            this.messages = messages;
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
    public IOpenLMessages getOpenLMessages() {
        return messages;
    }
}
