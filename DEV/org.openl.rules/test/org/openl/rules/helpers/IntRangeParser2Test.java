package org.openl.rules.helpers;

import org.junit.*;

import java.util.Random;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.junit.Assert.*;
import static org.openl.rules.helpers.IntRangeParser2.parse;

/**
 * Tests for {@link IntRangeParser2}
 */
public class IntRangeParser2Test {
    private static final Random random = new Random(0);

    private static final String[] NUMBERS_STR = { "-1 B", "-321K", "-1", "0", "1", "1234567", "999M" };
    private static final int[] NUMBERS_VALUES = { -1000000000, -321000, -1, 0, 1, 1234567, 999000000 };

    @Test
    public void testArbitrary() {
        // simple numbers
        assertIntRangeEquals("-1", -1, -1);
        assertIntRangeEquals("0", 0, 0);
        assertIntRangeEquals("1", 1, 1);
        assertIntRangeEquals("1234567", 1234567, 1234567);
        assertIntRangeEquals("-321K", -321000, -321000);
        assertIntRangeEquals("999M", 999000000, 999000000);
        assertIntRangeEquals("-1 B", -1000000000, -1000000000);
        // currency symbol
        assertIntRangeEquals("$13", 13, 13);
        assertIntRangeEquals("$13 - 200", 13, 200);
        assertIntRangeEquals("[$11; $32)", 11, 31);
        assertIntRangeEquals(">$2", 3, MAX_VALUE);
        assertIntRangeEquals("$2 +", 2, MAX_VALUE);
        assertIntRangeEquals("$2K+", 2000, MAX_VALUE);
        // ranges
        assertIntRangeEquals("[-100;100]", -100, 100);
        assertIntRangeEquals("(-100;100]", -99, 100);
        assertIntRangeEquals("(-100;100)", -99, 99);
        assertIntRangeEquals("[-100;100)", -100, 99);
        assertIntRangeEquals("(-0;10]", 1, 10);
        assertIntRangeEquals("[0; 10)", 0, 9);
        assertIntRangeEquals("[-10; -1]", -10, -1);
        assertIntRangeEquals("[1; 2]", 1, 2);
        assertIntRangeEquals("[3.. 4]", 3, 4);
        assertIntRangeEquals("4… 7", 5, 6);
        assertIntRangeEquals("4..7", 4, 7);
        assertIntRangeEquals("4...7", 5, 6);
        assertIntRangeEquals("[7-8]", 7, 8);
        assertIntRangeEquals("-15 - -8", -15, -8);
        assertIntRangeEquals("6-8", 6, 8);
        assertIntRangeEquals("-7-6", -7, 6);
        // spaces
        assertIntRangeEquals("-7 -6", -7, 6);
        assertIntRangeEquals("-7 - 6", -7, 6);
        assertIntRangeEquals("-7- 6", -7, 6);
        assertIntRangeEquals(" - 7 - 6 ", -7, 6);
        assertIntRangeEquals("4 .. 7", 4, 7);
        assertIntRangeEquals("4 ... 7", 5, 6);
        // plus and minus
        assertIntRangeEquals("+0+", 0, MAX_VALUE);
        assertIntRangeEquals("1+", 1, MAX_VALUE);
        assertIntRangeEquals("-1+", -1, MAX_VALUE);
        assertIntRangeEquals("18  +", 18, MAX_VALUE);
        assertIntRangeEquals("1M+", 1000000, MAX_VALUE);
        assertIntRangeEquals("1+ and 10 or less", 1, 10);
        assertIntRangeEquals("-7-+6", -7, 6);
        // one less/greater condition
        assertIntRangeEquals("0>", MIN_VALUE, -1);
        assertIntRangeEquals(">=1", 1, MAX_VALUE);
        assertIntRangeEquals(">1", 2, MAX_VALUE);
        assertIntRangeEquals("<10", MIN_VALUE, 9);
        assertIntRangeEquals("<=10", MIN_VALUE, 10);
        // two less/greater conditions
        assertIntRangeEquals(">0 and <10", 1, 9);
        assertIntRangeEquals("<=10 and >=0", 0, 10);
        assertIntRangeEquals("<=10 and >=0 and > 5", 6, 10);
        // less more
        assertIntRangeEquals("0 and more", 0, MAX_VALUE);
        assertIntRangeEquals("1 or less", MIN_VALUE, 1);
        assertIntRangeEquals("0 and more and 100 or less", 0, 100);
        assertIntRangeEquals("7 or less and -1 and more", -1, 7);
        assertIntRangeEquals("less than 0 ", MIN_VALUE, -1);
        assertIntRangeEquals("more than 0 ", 1, MAX_VALUE);
        assertIntRangeEquals("more than -1 less than 11", 0, 10);
        assertIntRangeEquals("more than 1000 and less than 10000", 1001, 9999);
        assertIntRangeEquals("less than -1 more than -1K", -999, -2);
        assertIntRangeEquals("-100 and more", -100, MAX_VALUE);
        assertIntRangeEquals("less than -10", MIN_VALUE, -11);
        assertIntRangeEquals("more than 2", 3, MAX_VALUE);
        assertIntRangeEquals("-100 and more less than 500", -100, 499);
        assertIntRangeEquals("-100 and more and less than 500", -100, 499);
        assertIntRangeEquals("41 or less and more than 31", 32, 41);
        assertIntRangeEquals("less than -10 more than -20", -19, -11);
        assertIntRangeEquals("more than 2 5 or less", 3, 5);
        assertIntRangeEquals("-100 and more less than 500", -100, 499);
        // thousands separator
        assertIntRangeEquals("123,456", 123456, 123456);
        assertIntRangeEquals("-123,456 - 987,654", -123456, 987654);
        assertIntRangeEquals(">123,456", 123457, MAX_VALUE);
        assertIntRangeEquals(">=123,456 <=987,654", 123456, 987654);
        assertIntRangeEquals("123,456 and more 987,654 or less", 123456, 987654);
        assertIntRangeEquals("123,456 and more 987,654 or less", 123456, 987654);
        assertIntRangeEquals("[123,456 - 987,654)", 123456, 987653);
        assertIntRangeEquals("123,456 and more 987,654 or less", 123456, 987654);
        // overflow test
        assertIntRangeEquals("2147483647", 2147483647, 2147483647);
        assertIntRangeEquals("-2147483648", -2147483648, -2147483648);
        assertWrong("-2147483649");
        assertWrong("2147483648");
        assertWrong("-10000000 K");
        // numeral adjectives
        assertIntRangeEquals("zero", 0, 0);
        assertIntRangeEquals("one .. fourteen", 1, 14);
        assertIntRangeEquals("twenty+", 20, MAX_VALUE);
        // empty input test
        assertWrong(null);
        assertWrong("");
        assertWrong(" ");
        assertWrong("  ");

        assertWrong("[1...5]");
        assertWrong("(1…5)");
        assertWrong("[-1; -10]");
        assertWrong("3 - 1");
        assertWrong("3..1");
        assertWrong("3...1");
        assertWrong("3…1");
        assertWrong("1…2");
        assertWrong("3 and more 1 or less");
    }

    @Test
    public void testOpenClosedIntervals() {
        // Now do the real testing
        for (boolean leftOpen : new boolean[] { false, true })
            for (boolean rightOpen : new boolean[] { false, true })
                for (String separator : new String[] { ";", "..", "-" })
                    for (int i = 0; i < NUMBERS_STR.length; i++)
                        for (int j = 0; j < NUMBERS_STR.length; j++)
                            if (NUMBERS_VALUES[i] + 1 < NUMBERS_VALUES[j]) {
                                String str = spaced(leftOpen ? "(" : "[") + NUMBERS_STR[i] + spaced(
                                    separator) + NUMBERS_STR[j] + spaced(rightOpen ? ")" : "]");
                                assertEquals(
                                    new IntRange(leftOpen ? NUMBERS_VALUES[i] + 1 : NUMBERS_VALUES[i],
                                        rightOpen ? NUMBERS_VALUES[j] - 1 : NUMBERS_VALUES[j]),
                                    parse(str));
                            }
    }

    @Test
    public void testAndMoreOrLessBig() {
        for (int i = 0; i < NUMBERS_STR.length; i++) {
            assertEquals(new IntRange(NUMBERS_VALUES[i], MAX_VALUE),
                parse(spaced(NUMBERS_STR[i]) + " and " + spaced("more")));
            assertEquals(new IntRange(MIN_VALUE, NUMBERS_VALUES[i]),
                parse(spaced(NUMBERS_STR[i]) + " or " + spaced("less")));
        }

        for (int i = 0; i < NUMBERS_STR.length; i++)
            for (int j = NUMBERS_STR.length - 1; j > 0 && NUMBERS_VALUES[j] >= NUMBERS_VALUES[i]; j--) {
                String s1 = concat(NUMBERS_STR[i], " and ", "more ", "and ", NUMBERS_STR[j], " or ", "less");
                String s2 = concat(NUMBERS_STR[j], " or ", "less ", "and ", NUMBERS_STR[i], " and ", "more");
                assertEquals(new IntRange(NUMBERS_VALUES[i], NUMBERS_VALUES[j]), parse(s1));
                assertEquals(new IntRange(NUMBERS_VALUES[i], NUMBERS_VALUES[j]), parse(s2));
            }
    }

    static String spaced(String value) {
        return randomSpaces() + value + randomSpaces();
    }

    static String concat(String... args) {
        StringBuilder b = new StringBuilder(1000);
        b.append(randomSpaces());
        for (String arg : args) {
            b.append(arg);
            b.append(randomSpaces());
        }
        return b.toString();
    }

    static String randomSpaces() {
        switch (random.nextInt(6)) {
            case 1:
                return " ";
            case 2:
                return "  ";
            case 3:
                return "   ";
            default:
                return "";
        }
    }

    void assertIntRangeEquals(String str, int low, int hi) {
        assertEquals("For string \"" + str + "\" expected range is [" + low + ";" + hi + "]",
            new IntRange(low, hi),
            parse(str));
    }

    void assertWrong(String str) {
        assertNull("Expected error", parse(str));
    }
}
