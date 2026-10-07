package org.openl.studio.repositories.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

class CreateFromProjectModelTest {

    @Test
    void sourceProjectIsRequired() {
        var violations = propertyViolations(new CreateFromProjectModel(null, " ", null, null, null));

        assertEquals(Set.of("sourceRepositoryId", "sourceProject"), violations);
    }

    @Test
    void validSourceProjectFieldsPassValidation() {
        var violations = propertyViolations(new CreateFromProjectModel("design", "Source", null, null, null));

        assertTrue(violations.isEmpty());
    }

    private static Set<String> propertyViolations(CreateFromProjectModel model) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(model).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
        }
    }
}
