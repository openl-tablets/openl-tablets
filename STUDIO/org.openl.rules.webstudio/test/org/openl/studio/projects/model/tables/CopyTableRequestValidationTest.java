package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies bean validation of the copy request: its name and module are required, a new-module path must be an xlsx
 * file, and validation cascades into each property.
 */
class CopyTableRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsAMinimalRequest() {
        var request = new CopyTableRequest("Module", null, null, "Copy", null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsProperties() {
        var request = new CopyTableRequest("Module", "Sheet", null, "Copy",
                List.of(new TableProperty("scope", "MODULE")));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankName() {
        var request = new CopyTableRequest("Module", null, null, " ", null);
        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void rejectsBlankModuleName() {
        var request = new CopyTableRequest(" ", null, null, "Copy", null);
        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void rejectsNonXlsxModulePath() {
        var request = new CopyTableRequest("Module", null, "rules/module.txt", "Copy", null);
        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void cascadesIntoProperties() {
        var request = new CopyTableRequest("Module", null, null, "Copy",
                List.of(new TableProperty(" ", "value")));
        assertEquals(1, validator.validate(request).size(), "a property with a blank name is rejected");
    }
}
