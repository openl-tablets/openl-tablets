package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class IntRangeParsingTest {

    @Test
    public void testDollarSymbol() {
        assertEquals(new IntRange(13, 200), new IntRange("$13 - 200"));
        assertEquals(new IntRange(11, 31), new IntRange("[$11; $32)"));
        assertEquals(new IntRange(3, Long.MAX_VALUE), new IntRange(">$2"));
        assertEquals(new IntRange(10, 10), new IntRange("$10"));
        assertEquals(new IntRange(2, Long.MAX_VALUE), new IntRange("$2 +"));
    }

    @Test
    public void testBrackets() {
        assertEquals(new IntRange(0, 6), new IntRange("[0..6 ]"));
        assertEquals(new IntRange(0, 6), new IntRange("[0..6 ]"));
        assertEquals(new IntRange(13, 200), new IntRange("[13; 200]"));
        assertEquals(new IntRange(11, 31), new IntRange("(10 .. 32)"));
        assertEquals(new IntRange(3, 4), new IntRange("(2;4]"));
        assertEquals(new IntRange(10, 100), new IntRange("[10 .. 101)"));
        assertEquals(new IntRange(-10, -1), new IntRange("[-10;0)"));
        assertEquals(new IntRange(-10, 1), new IntRange("[-10-2)"));
        assertEquals(new IntRange(-9, 2), new IntRange("(-10 - 2]"));
    }

    private void checkWrong(String x) {
        try {
            IntRangeParser.getInstance().parse(x);
            Assert.fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testFails() {
        checkWrong(",");
        checkWrong(",1");
        checkWrong("1,");
        checkWrong("1,1,");
        checkWrong(",1,1");

        checkWrong("[,1,1 .. 1]");
        checkWrong("[1,1 .. 1,]");
        checkWrong("[1,1 .. ,1]");
        checkWrong("[,1 .. 1]");
        checkWrong("[1, .. 1]");

        checkWrong(">,1");
        checkWrong("<1,");
        checkWrong("<,1,1,");
        checkWrong("<1,1,");
        checkWrong("<,1,1");

        checkWrong(",1,1 .. 1");
        checkWrong("1,1 .. 1,");
        checkWrong("1,1 .. ,1");
        checkWrong(",1 .. 1");
        checkWrong("1, .. 1");
    }

    @Test
    public void testExtraSpacesAndPluses() {
        assertEquals(new IntRange(3, 5), new IntRange("3 - 5"));
        assertEquals(new IntRange(100, Long.MAX_VALUE), new IntRange(">= 100"));
        assertEquals(new IntRange(2, Long.MAX_VALUE), new IntRange("2   +"));
    }

    @Test
    public void testJustNumber() {
        assertEquals(new IntRange(37, 37), new IntRange("37"));
    }

    @Test
    public void testKMB() {
        assertEquals(new IntRange(1000000, Long.MAX_VALUE), new IntRange("1M+"));
        assertEquals(new IntRange(1000000, Long.MAX_VALUE), new IntRange("  1M+  "));
        assertEquals(new IntRange(2000000000, 2000000000), new IntRange("2B"));
        assertEquals(new IntRange(1000, 36000000), new IntRange("1K .. 36M"));
        assertEquals(new IntRange(1000, 36000000), new IntRange("  1K   ..   36M  "));
        assertEquals(new IntRange(Long.MIN_VALUE, 24000), new IntRange("<=24K"));
    }

    @Test
    public void testMinMaxFormat() {
        assertEquals(new IntRange(3, 15), new IntRange("3..15"));
        assertEquals(new IntRange(1, 2), new IntRange("1-2"));
        assertEquals(new IntRange(13, 200), new IntRange("13 .. 200"));
        assertEquals(new IntRange(13, 200), new IntRange("  13   ..   200  "));
        assertEquals(new IntRange(14, 99), new IntRange("13 ... 100"));
        assertEquals(new IntRange(14, 99), new IntRange("13 … 100"));
        assertEquals(new IntRange(13, 19), new IntRange("[13 .. 20)"));
        assertEquals(new IntRange(14, 19), new IntRange("(13 .. 20)"));
        assertEquals(new IntRange(14, 19), new IntRange("  (  13   ..   20  )  "));
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals(new IntRange(Long.MIN_VALUE, 11), new IntRange("<12"));
        assertEquals(new IntRange(Long.MIN_VALUE, 7), new IntRange("<=7"));
        assertEquals(new IntRange(3, Long.MAX_VALUE), new IntRange(">2"));
    }

    @Test
    public void testMoreLessFormatBothBounds() {
        assertEquals(new IntRange(5, 11), new IntRange(">=5 <12"));
        assertEquals(new IntRange(4, 7), new IntRange("<=7 >3"));
        assertEquals(new IntRange(3, 8), new IntRange(" > 2   < 9 "));
        assertEquals(new IntRange(2, 9), new IntRange(" >= 2   <=9 "));
    }

    @Test
    public void testPlusFormat() {
        assertEquals(new IntRange(0, Long.MAX_VALUE), new IntRange("0+"));
    }

    @Test
    public void testSignedNumber() {
        assertEquals(new IntRange(-15, -8), new IntRange("-15 - -8"));
        assertEquals(new IntRange(-100, Long.MAX_VALUE), new IntRange("-100+"));
        assertEquals(new IntRange(3, Long.MAX_VALUE), new IntRange(">2"));
        assertEquals(new IntRange(-10, -10), new IntRange("-10"));
        assertEquals(new IntRange(-4, 2), new IntRange("-4-2"));
        assertEquals(new IntRange(-4, 2), new IntRange("[-4-2]"));
    }

    @Test
    public void testThousandsSeparator() {
        assertEquals(new IntRange(-123456, 987654), new IntRange("-123,456 - 987,654"));
        assertEquals(new IntRange(123456, Long.MAX_VALUE), new IntRange("123,456+"));
        assertEquals(new IntRange(123457, Long.MAX_VALUE), new IntRange(">123,456"));
        assertEquals(new IntRange(123456, 123456), new IntRange("123,456"));
        assertEquals(new IntRange(123456, 987653), new IntRange("[123,456 - 987,654)"));
        assertEquals(new IntRange(123456, 987654), new IntRange(">=123,456 <=987,654"));
        assertEquals(new IntRange(123456, 987654), new IntRange("123,456 and more 987,654 or less"));
        assertEquals(new IntRange(123456, 987654), new IntRange("  123,456   and   more   987,654   or   less  "));
    }

    @Test
    public void testVerbal() {
        assertEquals(new IntRange(-100, Long.MAX_VALUE), new IntRange("-100 and more"));
        assertEquals(new IntRange(-100, Long.MAX_VALUE), new IntRange("  -100   and   more  "));
        assertEquals(new IntRange(3, Long.MAX_VALUE), new IntRange("more than 2"));
        assertEquals(new IntRange(3, Long.MAX_VALUE), new IntRange("  more   than   2  "));
        assertEquals(new IntRange(Long.MIN_VALUE, -11), new IntRange("less than -10"));
        assertEquals(new IntRange(Long.MIN_VALUE, -11), new IntRange("  less   than   -10  "));
    }

    @Test
    public void testVerbalBothBounds() {
        assertEquals(new IntRange(-100, 499), new IntRange("-100 and more less than 500"));
        assertEquals(new IntRange(-100, 499), new IntRange("  -100   and   more   less   than   500  "));
        assertEquals(new IntRange(3, 5), new IntRange("more than 2 5 or less"));
        assertEquals(new IntRange(3, 5), new IntRange("  more   than   2   5   or   less  "));
        assertEquals(new IntRange(-19, -11), new IntRange("less than -10 more than -20"));
        assertEquals(new IntRange(-19, -11), new IntRange("  less   than   -10   more   than   -20  "));
        // assertEquals(new IntRange(32, 41), new IntRange("41 or less and more than 31"));
    }

    @Test
    public void testRangeSuffixies() {
        IntRange range = new IntRange("6-8");
        assertEquals(8, range.getMax());
        assertEquals(6, range.getMin());
    }

    @Test
    public void testInvalidIntRangeParsing1() {
        TestUtils.assertEx("test/rules/helpers/IntRangeParsing1.xls", "IntRangeParsing1.xls?sheet=hello2&range=C8:D8");
    }

    public interface ITestI {
        String hello1(int hour);
    }
}
