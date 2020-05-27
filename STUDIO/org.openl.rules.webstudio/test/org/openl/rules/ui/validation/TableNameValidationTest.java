package org.openl.rules.ui.validation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableNameValidationTest {

    private TableNameValidator validator = new TableNameValidator();

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
