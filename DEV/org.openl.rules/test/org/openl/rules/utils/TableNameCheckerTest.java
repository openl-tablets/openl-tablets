package org.openl.rules.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TableNameCheckerTest {

    @Test
    public void testWithLegalSign() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("a$c"));
    }

    @Test
    public void testNameWithUnderscore() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("_ac"));
    }

    @Test
    public void testWithBrackets() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier("ac()"));

        assertTrue(TableNameChecker.isInvalidJavaIdentifier("ac([}{])ac"));
    }

    @Test
    public void testStartsWithNumber() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier("2ac134"));
    }

    @Test
    public void testContainsNumber() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("a2c"));
    }

    @Test
    public void testIllegalSign() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier("tes/?s"));
    }

    @Test
    public void testNonLatinSymbols() {
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("мояПеременная"));
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("мойМетод"));

        assertFalse(TableNameChecker.isInvalidJavaIdentifier("αMethod"));
        assertFalse(TableNameChecker.isInvalidJavaIdentifier("εβδομάδα"));
    }

    @Test
    public void testNullAndEmptyString() {
        assertTrue(TableNameChecker.isInvalidJavaIdentifier(null));
        assertTrue(TableNameChecker.isInvalidJavaIdentifier(""));
    }

    @Test
    public void emptyCharacters() {
        TableNameChecker.isValidJavaIdentifier("       af");
    }

}
