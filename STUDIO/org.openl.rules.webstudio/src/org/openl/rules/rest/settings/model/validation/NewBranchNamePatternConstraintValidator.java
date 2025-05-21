package org.openl.rules.rest.settings.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.openl.util.StringUtils;

public class NewBranchNamePatternConstraintValidator implements ConstraintValidator<NewBranchNamePatternConstraint, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        ctx.disableDefaultConstraintViolation();
        var newBranchName = value.replace("{project-name}", "project-name")
                .replace("{username}", "username")
                .replace("{current-date}", "current-date");
        if (newBranchName.contains("{") || newBranchName.contains("}")) {
            ctx.buildConstraintViolationWithTemplate("Only the following placeholder options are available: {project-name}, {username}, {current-date}")
                    .addConstraintViolation();
        }
        if (!newBranchName.matches("[^\\\\:*?\"<>|{}~^\\s]*")) {
            ctx.buildConstraintViolationWithTemplate("Invalid branch name. Must not contain whitespaces or following characters: \\ : * ? \" < > | { } ~ ^")
                    .addConstraintViolation();
            return false;
        }
        if (newBranchName.contains("..") || newBranchName.contains("//")) {
            ctx.buildConstraintViolationWithTemplate("Invalid branch name. Should not contain consecutive '.' or '/' characters.")
                    .addConstraintViolation();
            return false;
        }
        if (!newBranchName.matches("^[^./].*[^./]")) {
            ctx.buildConstraintViolationWithTemplate("Invalid branch name. Cannot start with '.' or '/'.")
                    .addConstraintViolation();
            return false;
        }
        if (newBranchName.contains(".lock/") || newBranchName.endsWith(".lock")) {
            ctx.buildConstraintViolationWithTemplate("Invalid branch name. Should not contain '.lock/' or end with '.lock'.")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
