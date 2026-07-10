package org.openl.studio.repositories.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

class CreateFromRepositoryModelTest {

    @Test
    void pathIsValidated() {
        var violations = propertyViolations(new CreateFromRepositoryModel("/folder"));

        assertEquals(Set.of("path"), violations);
    }

    @Test
    void validPathPassesValidation() {
        var violations = propertyViolations(new CreateFromRepositoryModel("parent/folder"));

        assertTrue(violations.isEmpty());
    }

    private static Set<String> propertyViolations(CreateFromRepositoryModel model) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(model).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
        }
    }
}
