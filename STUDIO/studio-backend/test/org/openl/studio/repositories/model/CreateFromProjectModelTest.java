package org.openl.studio.repositories.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import org.openl.rules.workspace.ProjectIds;

class CreateFromProjectModelTest {

    @Test
    void sourceProjectIsRequired() {
        var violations = propertyViolations(new CreateFromProjectModel(null, " ", null, null, null));

        assertEquals(Set.of("sourceProject"), violations);
    }

    @Test
    void validSourceProjectFieldsPassValidation() {
        // The id names the repository of the source project too.
        var violations = propertyViolations(
                new CreateFromProjectModel(ProjectIds.of("design", "Source"), null, null, null));

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
