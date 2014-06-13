package org.openl.rules.convertor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NumberFormatHelperTest {

    private Locale defaultLocale;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);

    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testParse() {
        Number res = new NumberFormatHelper().parse("3.1415", null);
        assertEquals(Double.valueOf(3.1415), res);
    }

    @Test
    public void testParseNegative() {
        Number res = new NumberFormatHelper().parse("-2.1415", null);
        assertEquals(Double.valueOf(-2.1415), res);
    }

    @Test
    public void testParseWithoutLeadingZero() {
        Number res = new NumberFormatHelper().parse("-.123456789", null);
        assertEquals(Double.valueOf(-.123456789), res);
    }

    @Test
    public void testParseWithLeadingZeros() {
        Number res = new NumberFormatHelper().parse("0001.111000", null);
        assertEquals(Double.valueOf(1.111), res);
    }

    @Test
    public void testParsePrecision() {
        Number res = new NumberFormatHelper().parse("-1.99999999999999934", null);
        assertEquals(Double.valueOf(-1.99999999999999934), res);
    }

    @Test
    public void testParseLong() {
        Number res = new NumberFormatHelper().parse("-9223372036854775808", null);
        assertEquals(Long.valueOf(-9223372036854775808L), res);
    }

    @Test
    public void testParseExcessLong() {
        Number res = new NumberFormatHelper().parse("9223372036854775808", null);
        assertEquals(Double.valueOf(9223372036854775808d), res);
    }

    @Test
    public void testParsePercent() {
        Number res = new NumberFormatHelper().parse("17.5%", null);
        assertEquals(Double.valueOf(0.175), res);
    }

    @Test
    public void testParseE() {
        Number res = new NumberFormatHelper("0.0").parse("1.234E2", null);
        assertEquals(Double.valueOf(123.4), res);
    }

    @Test
    public void testParseENegative() {
        Number res = new NumberFormatHelper("0").parse("-1.23E-3", null);
        assertEquals(Double.valueOf(-0.00123), res);
    }

    @Test
    public void testParseELong() {
        Number res = new NumberFormatHelper().parse("-1.23E4", null);
        assertEquals(Long.valueOf(-12300), res);
    }

    @Test
    public void testParseWithFormat() {
        Number res = new NumberFormatHelper().parse("-3.1415$", "#,###$");
        assertEquals(Double.valueOf(-3.1415), res);
    }

    @Test
    public void testFormat() {
        String res = new NumberFormatHelper().format(3.135, null);
        assertEquals("3.135", res);
    }

    @Test
    public void testDefaultFormat() {
        String res = new NumberFormatHelper("0.##").format(3.135, null);
        assertEquals("3.14", res);
    }

    @Test
    public void testFormatWithFormat() {
        String res = new NumberFormatHelper().format(3.10, "#,000.0#$");
        assertEquals("003.1$", res);
    }

    @Test
    public void testFormatWithFormat2() {
        String res = new NumberFormatHelper().format(1234.125, "#,000.0#");
        assertEquals("1,234.12", res);
    }

    @Test
    public void testParseNull() {
        Number res = new NumberFormatHelper().parse(null, null);
        assertNull(res);
    }

    @Test
    public void testFormatNull() {
        String res = new NumberFormatHelper().format(null, null);
        assertNull(res);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseEmpty() {
        new NumberFormatHelper().parse("", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotNumber() {
        new NumberFormatHelper().parse("0L", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotENumber() {
        new NumberFormatHelper().parse("1e1", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseWithSpaces() {
        new NumberFormatHelper().parse("1 ", null);
    }
}
