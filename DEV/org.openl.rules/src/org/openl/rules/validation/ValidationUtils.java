package org.openl.rules.validation;

import java.util.Collection;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    static ValidationResult withMessages(Collection<OpenLMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return validationSuccess();
        }
        Collection<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
        if (errorMessages.isEmpty()) {
            return new ValidationResult(ValidationStatus.SUCCESS, messages);
        } else {
            return new ValidationResult(ValidationStatus.FAIL, messages);
        }
    }

    public static ValidationResult validationSuccess() {
        return new ValidationResult(ValidationStatus.SUCCESS);
    }

}
