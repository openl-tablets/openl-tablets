package org.openl.rules.validation;

import org.openl.message.OpenLMessage;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;

public class ValidationUtils {

    public static void addValidationMessage(ValidationResult validationResult, OpenLMessage message) {
        validationResult.getMessages().add(message);
    }

    public static ValidationResult validationSuccess() {
        return new ValidationResult(ValidationStatus.SUCCESS, null);
    }

}
