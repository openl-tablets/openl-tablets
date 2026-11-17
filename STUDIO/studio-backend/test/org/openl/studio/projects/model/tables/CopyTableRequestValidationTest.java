package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies bean validation of the copy request: its module is required, a name given must be one OpenL compiles, a
 * new-module path must be an xlsx file or a folder, and validation cascades into each property.
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

    /** Whether a copy needs a name depends on the kind of the table, which is checked once the table is known. */
    @Test
    void acceptsARequestWithoutAName() {
        var blank = new CopyTableRequest("Module", null, null, " ", null);
        assertNull(blank.name(), "a blank name is no name");
        assertTrue(validator.validate(blank).isEmpty());
        assertTrue(validator.validate(new CopyTableRequest("Module", null, null, null, null)).isEmpty());
    }

    @Test
    void rejectsAnInvalidName() {
        var request = new CopyTableRequest("Module", null, null, "1Copy", null);
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
    void acceptsAFolderOfANewMarkdownModule() {
        var request = new CopyTableRequest("Module", null, "rules/Rating/", "Copy", null);
        assertTrue(validator.validate(request).isEmpty());
    }

    /** A folder named by slashes alone would be the project folder itself, whose every Markdown file is read. */
    @Test
    void rejectsAModulePathOfSlashesAlone() {
        for (var path : List.of("/", "///", "")) {
            var request = new CopyTableRequest("Module", null, path, "Copy", null);
            assertEquals(1, validator.validate(request).size(), path);
        }
    }

    @Test
    void cascadesIntoProperties() {
        var request = new CopyTableRequest("Module", null, null, "Copy",
                List.of(new TableProperty(" ", "value")));
        assertEquals(1, validator.validate(request).size(), "a property with a blank name is rejected");
    }
}
