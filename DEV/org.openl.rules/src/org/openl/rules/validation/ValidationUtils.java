package org.openl.rules.validation;

import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;

public class ValidationUtils {

    public static ValidationResult validationSuccess() {
        return new ValidationResult(ValidationStatus.SUCCESS);
    }

}
