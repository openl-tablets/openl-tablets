package org.openl.rules.test.liveexcel.ranges;

import org.junit.Test;
import org.openl.rules.liveexcel.ranges.DoubleRangeParser;

import static org.junit.Assert.*;

public class DoubleRangeParsingTest {

    @Test
    public void testMinMaxFormat() {
        assertTrue(new DoubleRangeParser().parse("-1.0-2.0").contains(2));
        assertTrue(new DoubleRangeParser().parse("13 - 200").contains(34));
        assertTrue(new DoubleRangeParser().parse("-10--9.2").contains(-9.3));
    }

    @Test
    public void testPrefixFormat() {
        assertTrue(new DoubleRangeParser().parse("<12.01").contains(12));
        assertTrue(new DoubleRangeParser().parse("<= 7").contains(7));
        assertTrue(new DoubleRangeParser().parse(">-2").contains(-1.99999999999));
        assertTrue(new DoubleRangeParser().parse(">=3").contains(3));
    }

    @Test
    public void testPostfixFormat() {
        assertTrue(new DoubleRangeParser().parse("0+").contains(0.0000000001));
        assertTrue(new DoubleRangeParser().parse("-12 >").contains(-11.9999999999));
        assertTrue(new DoubleRangeParser().parse("1.1<").contains(1));
    }

    @Test
    public void testJustNumber() {
        assertTrue(new DoubleRangeParser().parse("- 37").contains(-37));
        assertTrue(new DoubleRangeParser().parse("22.12345").contains(22.12345));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailureParse() {
        new DoubleRangeParser().parse("--1");
    }

    @Test
    public void testKMB() {
        assertTrue(new DoubleRangeParser().parse("1M+").contains(1000001));
        assertTrue(new DoubleRangeParser().parse("2.0B").contains(2000000000));
        assertTrue(new DoubleRangeParser().parse("1.1K - 36.001M").contains(36001000));
    }

    @Test
    public void testDollarSymbol() {
        assertTrue(new DoubleRangeParser().parse("$13 - 200").contains(13));
        assertTrue(new DoubleRangeParser().parse(">$2").contains(3));
        assertTrue(new DoubleRangeParser().parse("$10").contains(10));
    }

    @Test
    public void testExtraSpacesAndPluses() {
        assertTrue(new DoubleRangeParser().parse("3 - 5").contains(4.999999));
        assertTrue(new DoubleRangeParser().parse(">=     -100").contains(-100));
        assertTrue(new DoubleRangeParser().parse("2  +").contains(155123));
    }
}
