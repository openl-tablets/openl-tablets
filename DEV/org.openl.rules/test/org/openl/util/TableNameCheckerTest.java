package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TableNameCheckerTest {

    @Test
    void testWithLegalSign() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("a$c"));
    }

    @Test
    void testNameWithUnderscore() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("_ac"));
    }

    @Test
    void testWithBrackets() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier("ac()"));

        assertTrue(TableNameChecker.isInvalidJavaIdentifier("ac([}{])ac"));
    }

    @Test
    void testStartsWithNumber() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier("2ac134"));
    }

    @Test
    void testContainsNumber() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("a2c"));
    }

    @Test
    void testIllegalSign() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier("tes/?s"));
    }

    @Test
    void testNonLatinSymbols() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("мояПеременная"));
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("мойМетод"));

        assertFalse(TableNameChecker.isInvalidJavaIdentifier("αMethod"));
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("εβδομάδα"));
    }

    @Test
    void testNullAndEmptyString() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier(null));
        assertTrue(TableNameChecker.isInvalidJavaIdentifier(""));
    }

    @Test
    void emptyCharacters() {
        TableNameChecker.isValidJavaIdentifier("       af");
    }

}
