package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuleRowHelperTest {

    @Test
    void nothingToReadIsNotNumeric() {
        assertFalse(RuleRowHelper.isNumeric(null));
        assertFalse(RuleRowHelper.isNumeric(""));
    }

    @Test
    void onlyDigitsAreNumeric() {
        assertTrue(RuleRowHelper.isNumeric("1"));
        assertTrue(RuleRowHelper.isNumeric("20260919"));
    }

    @Test
    void anythingButADigitIsNotNumeric() {
        assertFalse(RuleRowHelper.isNumeric("1a"));
        assertFalse(RuleRowHelper.isNumeric("-1"));
        // A separator is not a digit either, so a decimal is not read as a number here.
        assertFalse(RuleRowHelper.isNumeric("12.30"));
    }
}
