package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.util.Strings.*;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

public class StringsTest {

    @Test
    public void testContains() {
        // String
        assertFalse(contains((String)null, (String)null));
        assertFalse(contains(null, ""));
        assertFalse(contains(null, "s"));
        assertFalse(contains("", (String)null));
        assertTrue(contains("", ""));
        assertFalse(contains("", "s"));
        assertFalse(contains("asd", (String)null));
        assertTrue(contains("asd", ""));
        assertTrue(contains("asd", "a"));
        assertTrue(contains("asd", "s"));
        assertTrue(contains("asd", "d"));
        assertTrue(contains("Testing string value", "string"));
        assertFalse(contains("Testing string value", "strong"));

        // char
        assertFalse(contains(null, 's'));
        assertFalse(contains("", 's'));
        assertTrue(contains("asd", 'a'));
        assertTrue(contains("asd", 's'));
        assertTrue(contains("asd", 'd'));
        assertFalse(contains("asd", 'f'));
    }

    @Test
    public void testContainsAny() {
        // String
        assertFalse(containsAny(null, (String) null));
        assertFalse(containsAny(null, ""));
        assertFalse(containsAny(null, "s"));
        assertFalse(containsAny("", (String) null));
        assertFalse(containsAny("", ""));
        assertFalse(containsAny("", "s"));
        assertFalse(containsAny("asd", (String) null));
        assertFalse(containsAny("asd", ""));
        assertTrue(containsAny("asd", "a"));
        assertTrue(containsAny("asd", "s"));
        assertTrue(containsAny("asd", "d"));
        assertFalse(containsAny("asd", "f"));
        assertTrue(containsAny("asd", "edc"));
        assertFalse(containsAny("asd", "qwe"));
        assertTrue(containsAny("Testing string value", "string"));
        assertTrue(containsAny("Testing string value", "strong"));

        // char
        assertFalse(containsAny(null));
        assertFalse(containsAny(null, (char[]) null));
        assertFalse(containsAny(null, new char[0]));
        assertFalse(containsAny(null, 's'));
        assertFalse(containsAny("", (char[]) null));
        assertFalse(containsAny("", new char[0]));
        assertFalse(containsAny("", 's'));
        assertFalse(containsAny("asd", (char[]) null));
        assertFalse(containsAny("asd", new char[0]));
        assertTrue(containsAny("asd", 'a'));
        assertTrue(containsAny("asd", 's'));
        assertTrue(containsAny("asd", 'd'));
        assertFalse(containsAny("asd", 'f'));
        assertTrue(containsAny("asd", 'e', 'd', 'c'));
        assertFalse(containsAny("asd", 'q', 'w', 'e'));
        assertTrue(containsAny("Testing string value", 's', 'i', 'g'));
        assertTrue(containsAny("Testing string value", 's', 'o', 'g'));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(""));
        assertTrue(isEmpty(" "));
        assertFalse(isEmpty("  str  "));
        assertFalse(isEmpty("str"));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(isNotEmpty(null));
        assertFalse(isNotEmpty(""));
        assertFalse(isNotEmpty(" "));
        assertTrue(isNotEmpty("  str  "));
        assertTrue(isNotEmpty("str"));
    }

    @Test
    public void testLength() {
        assertEquals(0, length(null));
        assertEquals(0, length(""));
        assertEquals(1, length(" "));
        assertEquals(7, length("  str  "));
        assertEquals(3, length("str"));
    }

    @Test
    public void testTrim() {
        assertNull(trim(null));
        assertEquals("", trim(""));
        assertEquals("", trim(" "));
        assertEquals("str", trim("  str  "));
        assertEquals("str", trim("str"));
    }

    @Test
    public void testStartsWith() {
        assertTrue(startsWith(null, null));
        assertFalse(startsWith(null, ""));
        assertFalse(startsWith(null, "asd"));
        assertFalse(startsWith("", null));
        assertTrue(startsWith("", ""));
        assertFalse(startsWith("", "asd"));
        assertFalse(startsWith("asd", null));
        assertTrue(startsWith("asd", ""));
        assertTrue(startsWith("asd", "a"));
        assertFalse(startsWith("asd", "s"));
        assertFalse(startsWith("asd", "d"));
        assertTrue(startsWith("asd", "asd"));
        assertTrue(startsWith("asd", "as"));
        assertFalse(startsWith("asd", "sd"));
        assertFalse(startsWith("asd", "asdf"));
    }

    @Test
    public void testEndWith() {
        assertTrue(endsWith(null, null));
        assertFalse(endsWith(null, ""));
        assertFalse(endsWith(null, "asd"));
        assertFalse(endsWith("", null));
        assertTrue(endsWith("", ""));
        assertFalse(endsWith("", "asd"));
        assertFalse(endsWith("asd", null));
        assertTrue(endsWith("asd", ""));
        assertFalse(endsWith("asd", "a"));
        assertFalse(endsWith("asd", "s"));
        assertTrue(endsWith("asd", "d"));
        assertTrue(endsWith("asd", "asd"));
        assertFalse(endsWith("asd", "as"));
        assertTrue(endsWith("asd", "sd"));
        assertFalse(endsWith("asd", "asdf"));
    }

    @Test
    public void testSubstring() {
        assertNull(substring(null, -1));
        assertNull(substring(null, 0));
        assertNull(substring(null, 1));
        assertEquals("", substring("", -1));
        assertEquals("", substring("", 0));
        assertEquals("", substring("", 1));
        assertEquals("asd", substring("asd", -4));
        assertEquals("asd", substring("asd", -3));
        assertEquals("sd", substring("asd", -2));
        assertEquals("d", substring("asd", -1));
        assertEquals("asd", substring("asd", 0));
        assertEquals("sd", substring("asd", 1));
        assertEquals("d", substring("asd", 2));
        assertEquals("", substring("asd", 3));
        assertEquals("", substring("asd", 4));
    }

    @Test
    public void testSubstringWithEnd() {
        assertNull(substring(null, -1, 0));
        assertNull(substring(null, 0, 1));
        assertNull(substring(null, 1, -1));
        assertEquals("", substring("", -1, 0));
        assertEquals("", substring("", 0, 1));
        assertEquals("", substring("", 1, -1));
        assertEquals("", substring("asd", -5, 0));
        assertEquals("", substring("asd", 0, -5));
        assertEquals("", substring("asd", 5, 0));
        assertEquals("asd", substring("asd", 0, 5));

        assertEquals("", substring("asd", -3, -3));
        assertEquals("a", substring("asd", -3, -2));
        assertEquals("as", substring("asd", -3, -1));
        assertEquals("", substring("asd", -3, 0));
        assertEquals("a", substring("asd", -3, 1));
        assertEquals("as", substring("asd", -3, 2));
        assertEquals("asd", substring("asd", -3, 3));

        assertEquals("", substring("asd", -2, -3));
        assertEquals("", substring("asd", -2, -2));
        assertEquals("s", substring("asd", -2, -1));
        assertEquals("", substring("asd", -2, 0));
        assertEquals("", substring("asd", -2, 1));
        assertEquals("s", substring("asd", -2, 2));
        assertEquals("sd", substring("asd", -2, 3));

        assertEquals("", substring("asd", -1, -3));
        assertEquals("", substring("asd", -1, -2));
        assertEquals("", substring("asd", -1, -1));
        assertEquals("", substring("asd", -1, 0));
        assertEquals("", substring("asd", -1, 1));
        assertEquals("", substring("asd", -1, 2));
        assertEquals("d", substring("asd", -1, 3));

        assertEquals("", substring("asd", 0, -3));
        assertEquals("a", substring("asd", 0, -2));
        assertEquals("as", substring("asd", 0, -1));
        assertEquals("", substring("asd", 0, 0));
        assertEquals("a", substring("asd", 0, 1));
        assertEquals("as", substring("asd", 0, 2));
        assertEquals("asd", substring("asd", 0, 3));

        assertEquals("", substring("asd", 1, -3));
        assertEquals("", substring("asd", 1, -2));
        assertEquals("s", substring("asd", 1, -1));
        assertEquals("", substring("asd", 1, 0));
        assertEquals("", substring("asd", 1, 1));
        assertEquals("s", substring("asd", 1, 2));
        assertEquals("sd", substring("asd", 1, 3));

        assertEquals("", substring("asd", 2, -3));
        assertEquals("", substring("asd", 2, -2));
        assertEquals("", substring("asd", 2, -1));
        assertEquals("", substring("asd", 2, 0));
        assertEquals("", substring("asd", 2, 1));
        assertEquals("", substring("asd", 2, 2));
        assertEquals("d", substring("asd", 2, 3));

        assertEquals("", substring("asd", 3, -3));
        assertEquals("", substring("asd", 3, -2));
        assertEquals("", substring("asd", 3, -1));
        assertEquals("", substring("asd", 3, 0));
        assertEquals("", substring("asd", 3, 1));
        assertEquals("", substring("asd", 3, 2));
        assertEquals("", substring("asd", 3, 3));
    }

    @Test
    public void testRemoveStart() {
        assertNull(removeStart(null, null));
        assertNull(removeStart(null, ""));
        assertNull(removeStart(null, "asd"));
        assertEquals("", removeStart("", null));
        assertEquals("", removeStart("", ""));
        assertEquals("", removeStart("", "asd"));
        assertEquals("asd", removeStart("asd", null));
        assertEquals("asd", removeStart("asd", ""));
        assertEquals("sd", removeStart("asd", "a"));
        assertEquals("asd", removeStart("asd", "s"));
        assertEquals("asd", removeStart("asd", "d"));
        assertEquals("", removeStart("asd", "asd"));
        assertEquals("d", removeStart("asd", "as"));
        assertEquals("asd", removeStart("asd", "sd"));
        assertEquals("asd", removeStart("asd", "asdf"));
    }

    @Test
    public void testRemoveEnd() {
        assertNull(removeEnd(null, null));
        assertNull(removeEnd(null, ""));
        assertNull(removeEnd(null, "asd"));
        assertEquals("", removeEnd("", null));
        assertEquals("", removeEnd("", ""));
        assertEquals("", removeEnd("", "asd"));
        assertEquals("asd", removeEnd("asd", null));
        assertEquals("asd", removeEnd("asd", ""));
        assertEquals("asd", removeEnd("asd", "a"));
        assertEquals("asd", removeEnd("asd", "s"));
        assertEquals("as", removeEnd("asd", "d"));
        assertEquals("", removeEnd("asd", "asd"));
        assertEquals("asd", removeEnd("asd", "as"));
        assertEquals("a", removeEnd("asd", "sd"));
        assertEquals("asd", removeEnd("asd", "asdf"));
    }

    @Test
    public void testLowerCase() {
        assertNull(lowerCase(null));
        assertEquals("", lowerCase(""));
        assertEquals(" ", lowerCase(" "));
        assertEquals("  asd  ", lowerCase("  asd  "));
        assertEquals("asd", lowerCase("asd"));
        assertEquals("  asd  ", lowerCase("  AsD  "));
        assertEquals("asd", lowerCase("ASD"));
    }

    @Test
    public void testUpperCase() {
        assertNull(upperCase(null));
        assertEquals("", upperCase(""));
        assertEquals(" ", upperCase(" "));
        assertEquals("  ASD  ", upperCase("  asd  "));
        assertEquals("ASD", upperCase("asd"));
        assertEquals("  ASD  ", upperCase("  AsD  "));
        assertEquals("ASD", upperCase("ASD"));
    }

    @Test
    public void testReplace() {
        assertNull(replace(null, null, null));
        assertNull(replace(null, "", ""));
        assertNull(replace(null, "asd", "xyz"));
        assertEquals("", replace("", null, null));
        assertEquals("", replace("", "", ""));
        assertEquals("", replace("", "asd", "xyz"));
        assertEquals("asd", replace("asd", null, null));
        assertEquals("asd", replace("asd", "", ""));
        assertEquals("asd", replace("asd", "asd", null));

        assertEquals("xyz", replace("asd", "asd", "xyz"));
        assertEquals("qxyzf", replace("qasdf", "asd", "xyz"));
        assertEquals("qf", replace("qasdf", "asd", ""));
        assertEquals("qasdf", replace("qasdf", "qsf", ""));
        assertEquals("xyzxyz", replace("asdasd", "asd", "xyz"));
        assertEquals("qxyzfxyzg", replace("qasdfasdg", "asd", "xyz"));
        assertEquals("qfg", replace("qasdfasdg", "asd", ""));
        assertEquals("qasdfasdg", replace("qasdfasdg", "qsf", ""));
    }

    @Test
    public void testReplaceMax() {
        assertNull(replace(null, null, null, 1));
        assertNull(replace(null, "", "", 1));
        assertNull(replace(null, "asd", "xyz", 1));
        assertEquals("", replace("", null, null, 1));
        assertEquals("", replace("", "", "", 1));
        assertEquals("", replace("", "asd", "xyz", 1));
        assertEquals("asd", replace("asd", null, null, 1));
        assertEquals("asd", replace("asd", "", "", 1));
        assertEquals("asd", replace("asd", "asd", null, 1));
        assertEquals("asd", replace("asd", "asd", "xyz", 0));

        assertEquals("xyz", replace("asd", "asd", "xyz", 1));
        assertEquals("qxyzf", replace("qasdf", "asd", "xyz", 1));
        assertEquals("qf", replace("qasdf", "asd", "", 1));
        assertEquals("qasdf", replace("qasdf", "qsf", "", 1));
        assertEquals("xyzasd", replace("asdasd", "asd", "xyz", 1));
        assertEquals("qxyzfasdg", replace("qasdfasdg", "asd", "xyz", 1));
        assertEquals("qfasdg", replace("qasdfasdg", "asd", "", 1));
        assertEquals("qasdfasdg", replace("qasdfasdg", "qsf", "", 1));
    }

    @Test
    public void testToString() {
        assertNull(Strings.toString(null));
        assertEquals("", Strings.toString(""));
        assertEquals("    ", Strings.toString("    "));
        assertEquals("1", Strings.toString(1));
        assertEquals("1", Strings.toString(1d));
        assertEquals("1", Strings.toString(1f));
        assertEquals("0", Strings.toString(0d));
        assertEquals("0", Strings.toString(0f));
        assertEquals("1000", Strings.toString(1000d));
        assertEquals("1000", Strings.toString(1000f));
        assertEquals("-1000", Strings.toString(-1000d));
        assertEquals("-1000", Strings.toString(-1000f));
        assertEquals("0.01", Strings.toString(0.01d));
        assertEquals("0.01", Strings.toString(0.01f));
        assertEquals("-0.01", Strings.toString(-0.01d));
        assertEquals("-0.01", Strings.toString(-0.01f));
        assertEquals("1000", Strings.toString(new BigDecimal("1000.000")));
        assertEquals("1000", Strings.toString(new BigDecimal("1000")));
        assertEquals("100.001", Strings.toString(new BigDecimal("100.00100")));
        assertEquals("true", Strings.toString(true));
        assertEquals("false", Strings.toString(false));
    }

    @Test
    public void testToInteger() {
        assertNull(toInteger(null));
        assertNull(toInteger(""));
        assertNull(toInteger(" "));
        assertNull(toInteger("  \t  "));
        assertEquals(Integer.valueOf(1), toInteger("1"));
        assertEquals(Integer.valueOf(0), toInteger("0"));
        assertEquals(Integer.valueOf(0), toInteger("0000"));
        assertEquals(Integer.valueOf(-1), toInteger("-1"));
        assertEquals(Integer.valueOf(10000000), toInteger("10000000"));
        assertEquals(Integer.valueOf(1), toInteger(" \n1 \t "));
        assertNull(toInteger("1.0"));
        assertNull(toInteger("a"));
        assertNull(toInteger("99999999999999999999"));
    }

    @Test
    public void testToDouble() {
        assertNull(toDouble(null));
        assertNull(toDouble(""));
        assertNull(toDouble(" "));
        assertNull(toDouble("  \t  "));
        assertEquals(Double.valueOf(1), toDouble("1"));
        assertEquals(Double.valueOf(0), toDouble("0"));
        assertEquals(Double.valueOf(0), toDouble("0000.0000"));
        assertEquals(Double.valueOf(-1), toDouble("-1"));
        assertEquals(Double.valueOf(10000000), toDouble("10000000"));
        assertEquals(Double.valueOf(1), toDouble("1.0"));
        assertEquals(Double.valueOf(0.01), toDouble("0.01"));
        assertEquals(Double.valueOf(-0.01), toDouble("-.01"));
        assertEquals(Double.valueOf(-10000000.1), toDouble("-10000000.1"));
        assertEquals(Double.valueOf(1.1), toDouble(" \n 1.1 \t  "));
        assertEquals(Double.valueOf(-100), toDouble("-1e2"));
        assertEquals(Double.valueOf(0.01), toDouble("1e-2"));
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), toDouble("1e1000"));
        assertNull(toDouble("a"));
        assertNull(toDouble("13.."));
    }

    @Test
    public void testToNumber() {
        assertNull(toNumber(null));
        assertNull(toNumber(""));
        assertNull(toNumber(" "));
        assertNull(toNumber("  \t  "));
        assertEquals(Long.valueOf(1), toNumber("1"));
        assertEquals(Long.valueOf(0), toNumber("0"));
        assertEquals(Long.valueOf(-1), toNumber("-1"));
        assertEquals(Long.valueOf(10000000), toNumber("10000000"));
        assertEquals(Long.valueOf(1), toNumber("1.0"));
        assertEquals(Double.valueOf(0.01), toNumber("0.01"));
        assertEquals(Double.valueOf(-0.01), toNumber("-.01"));
        assertEquals(Double.valueOf(-10000000.1), toNumber("-10000000.1"));
        assertEquals(Double.valueOf(1), toNumber("1.000000000000000000000000000000000000000000000000000000001"));
        assertEquals(Double.POSITIVE_INFINITY, toNumber("∞"));
        assertNull(toNumber("X"));
        assertNull(toNumber("13.."));
    }

    @Test
    public void testConcatenate() {
        assertNull(concatenate(null));
        assertNull(concatenate(new Object[0]));
        assertNull(concatenate(null));
        assertNull(concatenate(null, null));
        assertEquals("", concatenate(null, "", null));
        assertEquals("", concatenate(""));
        assertEquals(" ", concatenate(" "));
        assertEquals("  asd  ", concatenate("  asd  "));
        assertEquals("asd", concatenate("asd"));
        assertEquals("  AsD  ", concatenate("  AsD  "));
        assertEquals("1", concatenate(1));
        assertEquals("ASD12.0", concatenate("ASD", 1, 2.0));
        assertEquals("true12.03.0%SEN", concatenate(true, 1, 2.0, 3f, '%', "SEN"));
    }

    @Test
    public void testIsInteger() {
        assertFalse(isInteger(null));
        assertFalse(isInteger(""));
        assertFalse(isInteger("+"));
        assertFalse(isInteger("-"));
        assertFalse(isInteger(" "));
        assertFalse(isInteger("123-"));
        assertFalse(isInteger("123+"));
        assertFalse(isInteger("12+34"));
        assertFalse(isInteger("12,34"));
        assertFalse(isInteger("12-34"));
        assertFalse(isInteger("12.3"));
        assertFalse(isInteger(".123"));
        assertFalse(isInteger("+.123"));
        assertFalse(isInteger("-.123"));
        assertFalse(isInteger("\n"));
        assertFalse(isInteger("\n\r"));
        assertFalse(isInteger("\t"));
        assertFalse(isInteger("foo"));
        assertFalse(isInteger("++123"));
        assertFalse(isInteger("--123"));
        assertFalse(isInteger("-+123"));
        assertFalse(isInteger("123d"));
        assertFalse(isInteger("123 "));
        assertFalse(isInteger(" 123"));
        assertFalse(isInteger("12 34"));
        assertFalse(isInteger("12a34"));
        assertFalse(isInteger("a1234"));
        assertFalse(isInteger("1234b"));
        assertFalse(isInteger("1234l"));
        assertFalse(isInteger("1234L"));
        assertTrue(isInteger("1234"));
        assertTrue(isInteger("+1234"));
        assertTrue(isInteger("-1234"));
        assertTrue(isInteger("0"));
        assertTrue(isInteger("+0"));
        assertTrue(isInteger("-0"));
        assertFalse(isInteger(10.2));
        assertFalse(isInteger(10L));
        assertFalse(isInteger(10D));
        assertFalse(isInteger(10F));
        assertFalse(isInteger(new Object()));
        assertTrue(isInteger(1234));
    }

    @Test
    public void testIsNumber() {
        assertFalse(isNumber(null));
        assertFalse(isNumber(""));
        assertFalse(isNumber("+"));
        assertFalse(isNumber("-"));
        assertFalse(isNumber(" "));
        assertFalse(isNumber("."));
        assertFalse(isNumber("NAN"));
        assertFalse(isNumber("nAn"));
        assertFalse(isNumber("naN"));
        assertFalse(isNumber("123-"));
        assertFalse(isNumber("123+"));
        assertFalse(isNumber("12+34"));
        assertFalse(isNumber("12,34"));
        assertFalse(isNumber("12-34"));
        assertFalse(isNumber("\n"));
        assertFalse(isNumber("\n\r"));
        assertFalse(isNumber("\t"));
        assertFalse(isNumber("foo"));
        assertFalse(isNumber("++123"));
        assertFalse(isNumber("--123"));
        assertFalse(isNumber("-+123"));
        assertFalse(isNumber("123 "));
        assertFalse(isNumber(" 123"));
        assertFalse(isNumber("12 34"));
        assertFalse(isNumber("12a34"));
        assertFalse(isNumber("a1234"));
        assertFalse(isNumber("1234b"));
        assertTrue(isNumber("1234l"));
        assertTrue(isNumber("1234L"));
        assertFalse(isNumber("NaN1"));
        assertFalse(isNumber("1NaN"));
        assertFalse(isNumber("-NaN-"));
        assertFalse(isNumber("NaN+"));
        assertFalse(isNumber("NaN-"));
        assertFalse(isNumber("1.23.4"));
        assertFalse(isNumber("123.3E1."));
        assertFalse(isNumber("123.3E"));
        assertFalse(isNumber("123.3E1E1"));
        assertFalse(isNumber("123.+E1"));
        assertFalse(isNumber("123.-E1"));
        assertFalse(isNumber("123.3ED"));
        assertFalse(isNumber("123.3E-D"));
        assertFalse(isNumber("123.3E+F"));
        assertFalse(isNumber("+000E.123"));
        assertFalse(isNumber("-000E.123"));
        assertFalse(isNumber("123E4l"));
        assertFalse(isNumber(".E21"));
        assertTrue(isNumber("+.123"));
        assertTrue(isNumber("-.123"));
        assertTrue(isNumber("123d"));
        assertTrue(isNumber("123D"));
        assertTrue(isNumber("123f"));
        assertTrue(isNumber("123F"));
        assertTrue(isNumber("123.F"));
        assertTrue(isNumber(".123"));
        assertTrue(isNumber("12.3"));
        assertTrue(isNumber("1234"));
        assertTrue(isNumber("+1234"));
        assertTrue(isNumber("-1234"));
        assertTrue(isNumber("0"));
        assertTrue(isNumber("+0"));
        assertTrue(isNumber("-0"));
        assertFalse(isNumber("NaN"));
        assertFalse(isNumber("+NaN"));
        assertFalse(isNumber("-NaN"));
        assertTrue(isNumber("123.4E21"));
        assertTrue(isNumber("123.E21"));
        assertTrue(isNumber("123.E21"));
        assertTrue(isNumber("123.E21F"));
        assertTrue(isNumber("123.E21D"));
        assertTrue(isNumber("1234E5"));
        assertTrue(isNumber("1234E+5"));
        assertTrue(isNumber("1234E-5"));
        assertTrue(isNumber("-0001.123"));
        assertTrue(isNumber("-000.123"));
        assertTrue(isNumber("+00.123"));
        assertTrue(isNumber("+0002.123"));
        assertTrue(isNumber(10.2));
        assertTrue(isNumber(10L));
        assertTrue(isNumber(10D));
        assertTrue(isNumber(10F));
        assertFalse(isNumber(new Object()));
        assertTrue(isNumber(1234));
    }

    @Test
    public void testIsDate() {
        assertFalse(isDate("foo"));
        assertFalse(isDate(""));
        assertFalse(isDate(" "));
        assertFalse(isDate(null));
        assertFalse(isDate("12.30"));
        assertFalse(isDate("121222121"));
        assertFalse(isDate("2014-12-12"));
        assertFalse(isDate("06.27.2014"));
        assertTrue(isDate("06/17/2014"));
        assertFalse(isDate("31/17/2014"));
        assertTrue(isDate("2014-12-12", "yyyy-MM-dd"));
        assertFalse(isDate("2014-13-12", "yyyy-MM-dd"));
        assertTrue(isDate("06.27.2014", "MM.dd.yyyy"));
        assertFalse(isDate("fo.ba.2014", "MM.dd.yyyy"));
        assertTrue(isDate(new Date()));
        assertTrue(isDate(new java.sql.Date(1221212212)));
        assertFalse(isDate(new Object()));
    }

    @Test
    public void testParsePattern() {
        assertEquals("", parseLikePattern(""));
        assertEquals(".", parseLikePattern("?"));
        assertEquals("....", parseLikePattern("????"));
        assertEquals(".*", parseLikePattern("*"));
        assertEquals("\\d", parseLikePattern("#"));
        assertEquals("\\d\\d\\d\\d\\d", parseLikePattern("#####"));
        assertEquals("\\p{Alpha}", parseLikePattern("@"));
        assertEquals("\\p{Alpha}\\p{Alpha}\\p{Alpha}", parseLikePattern("@@@"));
        assertEquals("[A-Z]", parseLikePattern("[A-Z]"));
        assertEquals("[A-Z]+", parseLikePattern("[A-Z]+"));
        assertEquals("[^A-Z]", parseLikePattern("[!A-Z]"));
        assertEquals("F", parseLikePattern("F"));
        assertEquals("Foo\\-Bar", parseLikePattern("Foo-Bar"));
        assertEquals("a.*a", parseLikePattern("a*a"));
        assertEquals("a\\da", parseLikePattern("a#a"));
        assertEquals("\\d\\d\\d\\-\\d\\d..+", parseLikePattern("###-##??+"));
        assertEquals(".+@.+\\..+", parseLikePattern("?+\\@?+\\.?+"));
    }

    @Test
    public void testLike() {
        assertFalse(like("", "F"));
        assertTrue(like("", ""));
        assertTrue(like("F", "F"));
        assertFalse(like("F", "f"));
        assertFalse(like("F", "FFF"));
        assertTrue(like("aBBBa", "a*a"));
        assertTrue(like("F", "[A-Z]"));
        assertTrue(like("BAR+", "[A-Z]++"));
        assertFalse(like("F", "[!A-Z]"));
        assertTrue(like("a2a", "a#a"));
        assertFalse(like("aTa", "a#a"));
        assertTrue(like("aTa", "a@a"));
        assertFalse(like("a2a", "a@a"));
        assertTrue(like("aM5b", "a[L-P]#[!c-e]"));
        assertTrue(like("BAT123khg", "B?T*"));
        assertFalse(like("CAT123khg", "B?T*"));
        assertTrue(like("AE1234AE", "@@####@@"));
        assertFalse(like("1E1234AE", "@@####@@"));
        assertTrue(like("123-45AE", "###-##@@"));
        assertTrue(like("123-45AE", "###-##@@"));
        assertFalse(like("A23-45AE", "###-##@@"));
        assertTrue(like("123-45AE", "###-##??+"));
        assertTrue(like("123-45AE123", "###-##??+"));
        assertTrue(like("123-45-AE", "#+-#+-@+"));
        assertTrue(like("foo.bar@gmail.com", "*\\@*.*"));
        assertTrue(like("foo@bar.com", "?+\\@?+.?+"));
        assertFalse(like("foo.bar@gmail.", "?+\\@?+.?+"));
        assertFalse(like("foo.bar@gmailcom", "?+\\@?+.?+"));
        assertTrue(like("+38(099) 123-12-12", "+##(###) ###-##-##"));
        assertFalse(like("+38(099)123-12-12", "+7#(###)###-##-##"));
    }
}
