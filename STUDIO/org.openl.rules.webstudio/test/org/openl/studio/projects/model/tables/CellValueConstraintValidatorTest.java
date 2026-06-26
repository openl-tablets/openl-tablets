package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Verifies that only scalar cell values (and {@code null}) are accepted; objects and arrays are rejected.
 */
class CellValueConstraintValidatorTest {

    private final CellValueConstraintValidator validator = new CellValueConstraintValidator();

    @Test
    void acceptsScalarsAndNull() {
        assertTrue(validator.isValid(null, null), "null clears the cell");
        assertTrue(validator.isValid("text", null));
        assertTrue(validator.isValid(42, null));
        assertTrue(validator.isValid(3.14, null));
        assertTrue(validator.isValid(true, null));
        assertTrue(validator.isValid(new Date(), null), "a date is a valid cell value at the grid level");
    }

    @Test
    void rejectsObjectsAndArrays() {
        assertFalse(validator.isValid(Map.of("a", 1), null), "a JSON object is not a cell value");
        assertFalse(validator.isValid(List.of(1, 2), null), "a JSON array is not a cell value");
    }
}
