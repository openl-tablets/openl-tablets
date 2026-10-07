package org.openl.studio.projects.model.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

class RestoreProjectHistoryRequestTest {

    @Test
    void validatesVersion() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            assertTrue(validator.validate(new RestoreProjectHistoryRequest("Revision Version")).isEmpty());
            assertEquals(1, validator.validate(new RestoreProjectHistoryRequest(" ")).size());
        }
    }
}
