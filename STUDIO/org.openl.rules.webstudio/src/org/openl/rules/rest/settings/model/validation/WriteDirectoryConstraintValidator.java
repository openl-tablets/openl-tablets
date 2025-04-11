package org.openl.rules.rest.settings.model.validation;

import javax.faces.validator.ValidatorException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.rules.webstudio.util.WebStudioValidationUtils;

/**
 * Validates directory for write access. If specified folder is not writable the validation error will appears
 *
 * @see WebStudioValidationUtils
 */
public class WriteDirectoryConstraintValidator implements ConstraintValidator<WriteDirectoryConstraint, String> {

    private String directoryType;

    @Override
    public void initialize(WriteDirectoryConstraint constraintAnnotation) {
        directoryType = constraintAnnotation.directoryType();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        try {
            WebStudioValidationUtils.directoryValidator(value, directoryType);
            return true;
        } catch (ValidatorException e) {
            context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
            return false;
        }
    }
}
