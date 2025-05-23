package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * @author PUdalau
 */
public class DoubleRangeParsingTest {

    @Test
    public void testBrackets() {
        assertTrue(new DoubleRange("(.1; 3.01)").contains(3d));
        assertTrue(new DoubleRange("(2; 3.01)").contains(3d));
        assertTrue(new DoubleRange("(2;3.01)").contains(3d));
        assertFalse(new DoubleRange("(2;3.01)").contains(2d));
        assertFalse(new DoubleRange("(2;3.01)").contains(3.01d));
        assertTrue(new DoubleRange("[2 .. 3.01)").contains(2d));
        assertTrue(new DoubleRange("[2 .. 3.01]").contains(3.01d));
        assertEquals(new DoubleRange("[4;5]"), new DoubleRange(4, 5));
    }

    private void checkWrong(String x) {
        try {
            new DoubleRange(x);
            fail();
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testFails() {
        checkWrong(",");
        checkWrong(",1");
        checkWrong("1,");
        checkWrong("1.");
        checkWrong("..1");
        checkWrong("1,1,");
        checkWrong(",1,1");

        checkWrong("[,1,1 .. 1]");
        checkWrong("[1,1 .. 1,]");
        checkWrong("[1,1 .. ,1]");
        checkWrong("[,1 .. 1]");
        checkWrong("[1, .. 1]");
        checkWrong("[1. .. 1]");
        checkWrong("[..1. .. 1]");

        checkWrong(">,1");
        checkWrong("<1,");
        checkWrong("<1.");
        checkWrong("<..1");
        checkWrong("<,1,1,");
        checkWrong("<1,1,");
        checkWrong("<,1,1");

        checkWrong(",1,1 .. 1");
        checkWrong("1,1 .. 1,");
        checkWrong("1,1 .. ,1");
        checkWrong(",1 .. 1");
        checkWrong("1, .. 1");
        checkWrong("1. .. 1");
        checkWrong("..1 .. 1");

        checkWrong("1.0.0");
        checkWrong(",1.0");
        checkWrong("1.0,");
        checkWrong("1,1.0,");
        checkWrong(",1.0,1.0");

        checkWrong("[,1.0,1.0 .. 1]");
        checkWrong("[1,1.0 .. 1.0,]");
        checkWrong("[1,1.0 .. ,1]");
        checkWrong("[,1.0 .. 1]");
        checkWrong("[1, .. 1.0]");

        checkWrong(">,1.0");
        checkWrong("<1,");
        checkWrong("<,1.0,1,");
        checkWrong("<1,1.0,");
        checkWrong("<,1,1.0");

        checkWrong(",1,1.0 .. 1.0");
        checkWrong("1,1.0 .. 1.0,");
        checkWrong("1,1.0 .. ,1");
        checkWrong(",1.0 .. 1");
        checkWrong("1, .. 1.0");
    }

    @Test
    public void testJustNumber() {
        assertEquals(new DoubleRange(37.1, 37.1), new DoubleRange("37.1"));
    }

    @Test
    public void testKMB() {
        assertEquals(new DoubleRange(1100000, Double.POSITIVE_INFINITY), new DoubleRange("1.1M+"));
        assertEquals(new DoubleRange(2330000000d, 2330000000d), new DoubleRange("2.33B"));
        assertEquals(new DoubleRange(1200, 36000000), new DoubleRange("1.2K .. 36M"));
        assertEquals(new DoubleRange(Double.NEGATIVE_INFINITY, 24001), new DoubleRange("<=24.001K"));
    }

    @Test
    public void testThousandsSeparator() {
        assertEquals(new DoubleRange(-123456, 987654.3), new DoubleRange("-123,456 - 987,654.3"));
        assertEquals(new DoubleRange("123456.7+"), new DoubleRange("123,456.7+"));
        assertEquals(new DoubleRange(">123456.7"), new DoubleRange(">123,456.7"));
        assertEquals(new DoubleRange(123456.7, 123456.7), new DoubleRange("123,456.7"));
        assertEquals(new DoubleRange("[123456.7 - 987654)"), new DoubleRange("[123,456.7 - 987,654)"));
        assertEquals(new DoubleRange(123456.7, 987654), new DoubleRange(">=123,456.7 <=987,654"));
        assertEquals(new DoubleRange(123456.7, 987654), new DoubleRange("123,456.7 and more 987,654 or less"));
    }

    @Test
    public void testMinMaxFormat() {
        assertEquals(new DoubleRange(1, 2.3), new DoubleRange("1-2.3"));
        assertEquals(new DoubleRange(13.01, 200.7), new DoubleRange("13.01 .. 200.7"));
        assertEquals(new DoubleRange("(10; 123)"), new DoubleRange("10 ... 123"));
        assertEquals(new DoubleRange("(10; 123)"), new DoubleRange("10 … 123"));
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals(new DoubleRange("less than 12.123"), new DoubleRange("<12.123"));
        assertEquals(new DoubleRange("7 or less"), new DoubleRange("<=7"));
        assertEquals(new DoubleRange("more than 0.0000001"), new DoubleRange(">0.0000001"));
    }

    @Test
    public void testMoreLessFormatBothBounds() {
        assertEquals(new DoubleRange("[5.12; 12.123)"), new DoubleRange(">=5.12 <12.123"));
        assertEquals(new DoubleRange("(3; 7]"), new DoubleRange("<=7 >3"));
        assertEquals(new DoubleRange("(0.0000001; 9.0000001)"), new DoubleRange(" > 0.0000001   < 9.0000001 "));
        assertEquals(new DoubleRange("[0.0000001; 9.0000001]"), new DoubleRange(" >= 0.0000001   <=9.0000001 "));
    }

    @Test
    public void testPlusFormat() {
        assertEquals(new DoubleRange(123457890.0987654321, Double.POSITIVE_INFINITY),
                new DoubleRange("123457890.0987654321+"));
    }

    @Test
    public void testNegativeDoubleRange() {
        assertEquals(new DoubleRange(-200.7, -13.01), new DoubleRange("[-200.7;-13.01]"));
        assertEquals(new DoubleRange(Double.NEGATIVE_INFINITY, -7), new DoubleRange("<= -7"));
    }

    @Test
    public void testSimplifiedDeclaration() {
        DoubleRange range1 = new DoubleRange("1-15");
        assertEquals(range1, new DoubleRange("[1;15]"));
        assertTrue(range1.contains(1d));
        assertTrue(range1.contains(15d));
    }

    @Test
    public void testLiteralPreffixes() {
        assertEquals(new DoubleRange("less than 5"), new DoubleRange("<5"));
        assertEquals(new DoubleRange("  less   than   5"), new DoubleRange("<5"));

        assertEquals(new DoubleRange("more than 5"), new DoubleRange(">5"));
        assertEquals(new DoubleRange("  more   than   5  "), new DoubleRange(">5"));

        assertEquals(new DoubleRange("more \n    than 5"), new DoubleRange(">5"));
    }

    @Test
    public void testLiteralSuffixes() {
        assertEquals(new DoubleRange("10 or less"), new DoubleRange("<=10"));
        assertEquals(new DoubleRange("  10   or   less  "), new DoubleRange("<=10"));

        assertEquals(new DoubleRange("5 or less"), new DoubleRange("<=5"));
        assertEquals(new DoubleRange("  5   or   less  "), new DoubleRange("<=5"));

        assertEquals(new DoubleRange("5 or \n     less"), new DoubleRange("<=5"));

        assertEquals(new DoubleRange("10 and more"), new DoubleRange("10+"));
        assertEquals(new DoubleRange("  10   and   more  "), new DoubleRange("10+"));

        assertEquals(new DoubleRange("10 and \n      more"), new DoubleRange("10+"));

        assertEquals(new DoubleRange("5 and more"), new DoubleRange(">=5"));
        assertEquals(new DoubleRange("  5   and   more  "), new DoubleRange(">=5"));
    }

    @Test
    public void testVerbalBothBounds() {
        assertEquals(new DoubleRange("[-100.1; 500.2)"), new DoubleRange("-100.1 and more less than 500.2"));
        assertEquals(new DoubleRange("[-100.1; 500.2)"), new DoubleRange("  -100.1   and   more   less   than   500.2  "));

        assertEquals(new DoubleRange("(2; 5]"), new DoubleRange("more than 2 5 or less"));
        assertEquals(new DoubleRange("(2; 5]"), new DoubleRange("  more   than   2   5   or   less  "));

        assertEquals(new DoubleRange("(-20.5; -10.5)"), new DoubleRange("less than -10.5 more than -20.5"));
        assertEquals(new DoubleRange("(-20.5; -10.5)"), new DoubleRange("less   than   -10.5   more   than   -20.5  "));
    }

    @Test
    public void testIsTruncated() {
        assertFalse(DoubleRange.isTruncated(15.89f, 1.89));
        assertFalse(DoubleRange.isTruncated(15.89d, 1.89));
        assertTrue(DoubleRange.isTruncated(new BigDecimal("2e+308"), new BigDecimal("2e+308").doubleValue()));
        assertFalse(DoubleRange.isTruncated(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertFalse(DoubleRange.isTruncated(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }
}
