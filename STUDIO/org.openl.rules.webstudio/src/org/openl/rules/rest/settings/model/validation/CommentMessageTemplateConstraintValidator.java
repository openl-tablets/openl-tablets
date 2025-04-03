package org.openl.rules.rest.settings.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.openl.util.StringUtils;

public class CommentMessageTemplateConstraintValidator implements ConstraintValidator<CommentMessageTemplateConstraint, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        ctx.disableDefaultConstraintViolation();

        boolean isValid = true;
        if (!value.contains("{commit-type}")) {
            ctx.buildConstraintViolationWithTemplate("Comment message template must contain '{commit-type}'").addConstraintViolation();
            isValid = false;
        }
        if (!value.contains("{user-message}")) {
            ctx.buildConstraintViolationWithTemplate("Comment message template must contain '{user-message}'").addConstraintViolation();
            isValid = false;
        }
        return isValid;
    }
}
