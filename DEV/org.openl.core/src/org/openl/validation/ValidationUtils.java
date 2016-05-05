package org.openl.validation;

import java.util.ArrayList;
import java.util.List;

import org.openl.message.OpenLMessage;

public class ValidationUtils {

    public static void addValidationMessage(ValidationResult validationResult, OpenLMessage message) {
        validationResult.getMessages().add(message);
    }

    public static ValidationResult validationSuccess() {
        return new ValidationResult(ValidationStatus.SUCCESS, null);
    }

}
