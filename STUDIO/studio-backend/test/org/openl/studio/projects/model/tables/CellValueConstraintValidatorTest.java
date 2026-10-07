package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Verifies that scalar cell values and one-dimensional arrays of them are accepted; objects and nested arrays are
 * rejected.
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
    void acceptsOneDimensionalScalarArrays() {
        assertTrue(validator.isValid(List.of(1, 2), null), "a JSON array is a cell value");
        assertTrue(validator.isValid(new Object[]{"a", null, true}, null), "null slots are preserved between values");
    }

    @Test
    void rejectsEmptyArraysObjectsAndNestedArrays() {
        assertFalse(validator.isValid(List.of(), null), "an empty JSON array is indistinguishable from an empty cell");
        assertFalse(validator.isValid(new Object[0], null));
        assertFalse(validator.isValid(new Object[]{null}, null), "a singleton null is indistinguishable from an empty cell");
        assertFalse(validator.isValid(new Object[]{""}, null), "an empty string is normalized to null");
        assertFalse(validator.isValid(new Object[]{" value "}, null), "surrounding whitespace is trimmed by the parser");
        assertFalse(validator.isValid(Map.of("a", 1), null), "a JSON object is not a cell value");
        assertFalse(validator.isValid(List.of(Map.of("a", 1)), null), "an array cannot contain an object");
        assertFalse(validator.isValid(List.of(List.of(1, 2)), null), "a JSON array cannot be nested");
        assertFalse(validator.isValid(new Object[]{new Object[]{1, 2}}, null), "a Java array cannot be nested");
    }
}
