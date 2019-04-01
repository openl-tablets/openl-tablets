package org.openl.rules.calc.element;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpreadsheetExpressionMarkerTest {

    @Test
    public void testEmptyBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{}"));
    }

    @Test
    public void testSpaceInBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{ }"));
    }

    @Test
    public void testLetterInBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{L}"));
    }

    @Test
    public void testWordInBracket() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("{Word}"));
    }

    @Test
    public void testEqualDigit() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("=1"));
    }

    @Test
    public void testEqualLetter() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("=A"));
    }

    @Test
    public void testEqualWord() {
        assertTrue(SpreadsheetExpressionMarker.isFormula("=Word"));
    }

    @Test
    public void testEmptyEqual() {
        assertFalse(SpreadsheetExpressionMarker.isFormula("="));
    }

    @Test
    public void testOpenBracket() {
        assertFalse(SpreadsheetExpressionMarker.isFormula("{word"));
    }

    @Test
    public void testCloseBracket() {
        assertFalse(SpreadsheetExpressionMarker.isFormula("word}"));
    }
}
