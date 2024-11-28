package org.openl.rules.ui.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TableNameValidationTest {

    private final TableNameValidator validator = new TableNameValidator();

    @Test
    public void testValidName() {
        assertTrue(validator.isValid("xT", null));
        assertTrue(validator.isValid("мояТаблица", null));
        assertTrue(validator.isValid("$5af", null));
    }

    @Test
    public void testInvalidName() {
        assertFalse(validator.isValid("4xT", null));
        assertFalse(validator.isValid("666", null));
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid("    abc", null));
        assertFalse(validator.isValid("aB/c", null));
        assertFalse(validator.isValid("aB?c", null));
        assertFalse(validator.isValid("aB()", null));
    }
}
