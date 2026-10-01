package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.types.java.JavaOpenClass;

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

    static Stream<Arguments> wholeNumbers() {
        return Stream.of(
                Arguments.of(2147483647d, Integer.class, 2147483647),
                Arguments.of(2147483648d, Long.class, 2147483648L),
                Arguments.of(2147483648d, long.class, 2147483648L),
                Arguments.of(-2147483649d, Long.class, -2147483649L),
                Arguments.of(-0x1p63, Long.class, Long.MIN_VALUE),
                Arguments.of(2147483648d, BigInteger.class, new BigInteger("2147483648")),
                // A double keeps 17 significant digits, the same as BigDecimal reads from the text of the cell.
                Arguments.of(0x1p63, BigInteger.class, new BigInteger("9223372036854776000")),
                Arguments.of(1e20, BigInteger.class, new BigInteger("100000000000000000000")));
    }

    @ParameterizedTest
    @MethodSource("wholeNumbers")
    void wholeNumberIsReadIntoTheExpectedType(double number, Class<?> expectedType, Object expected) {
        var paramType = JavaOpenClass.getOpenClass(expectedType);
        assertEquals(expected, RuleRowHelper.loadNativeValue(numericCell(number), paramType));
    }

    @Test
    void numberOutOfTheExpectedRangeIsNotRead() {
        assertNull(RuleRowHelper.loadNativeValue(numericCell(2147483648d), JavaOpenClass.getOpenClass(Integer.class)));
        assertNull(RuleRowHelper.loadNativeValue(numericCell(0x1p63), JavaOpenClass.getOpenClass(Long.class)));
    }

    @Test
    void numberWithFractionIsNotReadAsWholeNumber() {
        assertNull(RuleRowHelper.loadNativeValue(numericCell(2147483648.5), JavaOpenClass.getOpenClass(Long.class)));
        assertNull(RuleRowHelper.loadNativeValue(numericCell(Double.POSITIVE_INFINITY),
                JavaOpenClass.getOpenClass(BigInteger.class)));
    }

    private static ICell numericCell(double number) {
        var cell = mock(ICell.class);
        when(cell.getNativeType()).thenReturn(IGrid.CELL_TYPE_NUMERIC);
        when(cell.getObjectValue()).thenReturn(number);
        when(cell.getNativeNumber()).thenReturn(number);
        return cell;
    }
}
