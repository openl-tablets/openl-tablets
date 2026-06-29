package org.openl.rules.calc.element;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpreadsheetExpressionMarkerTest {

    @Test
    void testEmptyBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{}"));
    }

    @Test
    void testSpaceInBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{ }"));
    }

    @Test
    void testLetterInBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{L}"));
    }

    @Test
    void testWordInBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{Word}"));
    }

    @Test
    void testEqualDigit() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("=1"));
    }

    @Test
    void testEqualLetter() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("=A"));
    }

    @Test
    void testEqualWord() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("=Word"));
    }

    @Test
    void testEmptyEqual() {
        assertFalse(SpreadsheetExpressionMarker.isFormula("="));
    }

    @Test
    void testOpenBracket() {
        assertFalse(SpreadsheetExpressionMarker.isFormula("{word"));
    }

    @Test
    void testCloseBracket() {
        assertFalse(SpreadsheetExpressionMarker.isFormula("word}"));
    }
}
