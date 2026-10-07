package org.openl.studio.projects.model.tables;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.jspecify.annotations.Nullable;

import org.openl.util.TableNameChecker;

/**
 * Checks that a table name is an identifier OpenL compiles a table under.
 *
 * @author Vladyslav Pikus
 */
public class TableNameConstraintValidator implements ConstraintValidator<TableNameConstraint, String> {

    @Override
    public boolean isValid(@Nullable String name, ConstraintValidatorContext context) {
        // A missing name is already reported by @NotBlank, which every name carries. The name is written to the
        // header as it is given, so the space around one is part of what is refused.
        return name == null || name.isBlank() || TableNameChecker.isValidJavaIdentifier(name);
    }
}
