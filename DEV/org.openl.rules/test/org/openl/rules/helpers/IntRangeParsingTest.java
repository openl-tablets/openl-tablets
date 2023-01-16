package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class IntRangeParsingTest {

    @Test
    public void testDollarSymbol() {
        assertEquals("[13..200]", new IntRange("$13 - 200").toString());
        assertEquals("[11..32)", new IntRange("[$11; $32)").toString());
        assertEquals("> 2", new IntRange(">$2").toString());
        assertEquals("10", new IntRange("$10").toString());
        assertEquals(">= 2", new IntRange("$2 +").toString());
    }

    @Test
    public void testBrackets() {
        assertEquals("[0..6]", new IntRange("[0..6 ]").toString());
        assertEquals("[0..6]", new IntRange("[0..6 ]").toString());
        assertEquals("[13..200]", new IntRange("[13; 200]").toString());
        assertEquals("(10..32)", new IntRange("(10 .. 32)").toString());
        assertEquals("(2..4]", new IntRange("(2;4]").toString());
        assertEquals("[10..101)", new IntRange("[10 .. 101)").toString());
        assertEquals("[-10..0)", new IntRange("[-10;0)").toString());
        assertEquals("[-10..2)", new IntRange("[-10-2)").toString());
        assertEquals("(-10..2]", new IntRange("(-10 - 2]").toString());
    }

    private void checkWrong(String x) {
        try {
            new IntRange(x);
            Assert.fail();
        } catch (Exception ignored) {
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
        assertEquals("[3..15]", new IntRange("3..15").toString());
        assertEquals("[1..2]", new IntRange("1-2").toString());
        assertEquals("[13..200]", new IntRange("13 .. 200").toString());
        assertEquals("[13..200]", new IntRange("  13   ..   200  ").toString());
        assertEquals("(13..100)", new IntRange("13 ... 100").toString());
        assertEquals("(13..100)", new IntRange("13 … 100").toString());
        assertEquals("[13..20)", new IntRange("[13 .. 20)").toString());
        assertEquals("(13..20)", new IntRange("(13 .. 20)").toString());
        assertEquals("(13..20)", new IntRange("  (  13   ..   20  )  ").toString());
    }

    @Test
    public void testMoreLessFormat() {
        assertEquals("< 12", new IntRange("<12").toString());
        assertEquals("<= 7", new IntRange("<=7").toString());
        assertEquals("> 2", new IntRange(">2").toString());
    }

    @Test
    public void testMoreLessFormatBothBounds() {
        assertEquals("[5..12)", new IntRange(">=5 <12").toString());
        assertEquals("(3..7]", new IntRange("<=7 >3").toString());
        assertEquals("(2..9)", new IntRange(" > 2   < 9 ").toString());
        assertEquals("[2..9]", new IntRange(" >= 2   <=9 ").toString());
    }

    @Test
    public void testPlusFormat() {
        assertEquals(new IntRange(0, Long.MAX_VALUE), new IntRange("0+"));
    }

    @Test
    public void testSignedNumber() {
        assertEquals(new IntRange(-15, -8), new IntRange("-15 - -8"));
        assertEquals(new IntRange(-15, -8), new IntRange("-15 --8"));
        assertEquals(new IntRange(-15, -8), new IntRange("-15- -8"));
        assertEquals(new IntRange(-100, Long.MAX_VALUE), new IntRange("-100+"));
        assertEquals("> 2", new IntRange(">2").toString());
        assertEquals("-10", new IntRange("-10").toString());
        assertEquals("[-4..2]", new IntRange("-4-2").toString());
        assertEquals("[-4..-2]", new IntRange("-4--2").toString());
        assertEquals("[-4..2]", new IntRange("[-4-2]").toString());
        assertEquals("[-4..-2]", new IntRange("[-4--2]").toString());
    }

    @Test
    public void testThousandsSeparator() {
        assertEquals(new IntRange(-123456, 987654), new IntRange("-123,456 - 987,654"));
        assertEquals(new IntRange(123456, Long.MAX_VALUE), new IntRange("123,456+"));
        assertEquals("> 123456", new IntRange(">123,456").toString());
        assertEquals("123456", new IntRange("123,456").toString());
        assertEquals("[123456..987654)", new IntRange("[123,456 - 987,654)").toString());
        assertEquals("[123456..987654]", new IntRange(">=123,456 <=987,654").toString());
        assertEquals("[123456..987654]", new IntRange("123,456 and more 987,654 or less").toString());
        assertEquals("[123456..987654]", new IntRange("  123,456   and   more   987,654   or   less  ").toString());
    }

    @Test
    public void testVerbal() {
        assertEquals(">= -100", new IntRange("-100 and more").toString());
        assertEquals(">= -100", new IntRange("  -100   and   more  ").toString());
        assertEquals("> 2", new IntRange("more than 2").toString());
        assertEquals("> 2", new IntRange("  more   than   2  ").toString());
        assertEquals("< -10", new IntRange("less than -10").toString());
        assertEquals("< -10", new IntRange("  less   than   -10  ").toString());
    }

    @Test
    public void testVerbalBothBounds() {
        assertEquals("[-100..500)", new IntRange("-100 and more less than 500").toString());
        assertEquals("[-100..500)", new IntRange("  -100   and   more   less   than   500  ").toString());
        assertEquals("(2..5]", new IntRange("more than 2 5 or less").toString());
        assertEquals("(2..5]", new IntRange("  more   than   2   5   or   less  ").toString());
        assertEquals("(-20..-10)", new IntRange("less than -10 more than -20").toString());
        assertEquals("(-20..-10)", new IntRange("  less   than   -10   more   than   -20  ").toString());
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
