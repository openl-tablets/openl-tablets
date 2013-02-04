package org.openl.rules.helpers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.ByteValue;
import org.openl.meta.LongValue;
import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.meta.number.NumberValue;
import org.openl.rules.TestHelper;

/**
 * Test to check that methods from {@link RulesUtils} and children of
 * {@link NumberValue} are visible and executed from excel.
 * 
 * @author DLiauchuk
 * 
 *         TODO: test all methods
 */
public class RulesUtilsTest {

    private static final String src = "test/rules/helpers/RulesUtilsTest.xlsx";

    private static TestInterf instance;
    private static String str;
    private static String nullStr = null;

    public interface TestInterf {
        String testMaxByte(byte[] obj);

        BigInteger testSum(BigInteger[] values);

        LongValue testQuaotient(ByteValue number, ByteValue divisor);

        ByteValue testMin(ByteValue[] values);

        BigDecimalValue testAvg(BigDecimalValue[] values);

        boolean checkOr();
    }

    @Before
    public void init() {
        if (instance == null) {
            File xlsFile = new File(src);
            TestHelper<TestInterf> testHelper;
            testHelper = new TestHelper<TestInterf>(xlsFile, TestInterf.class);
            str = "Testing string value";

            instance = testHelper.getInstance();
        }
    }

    @Test
    public void testByteMax() {
        assertEquals("res2", instance.testMaxByte(new byte[] { (byte) 3, (byte) 5 }));
    }

    @Test
    public void testBigIntegerSum() {
        assertEquals(BigInteger.valueOf(30),
            instance.testSum(new BigInteger[] { BigInteger.valueOf(10), BigInteger.valueOf(5), BigInteger.valueOf(15) }));
    }

    @Test
    public void testByteValueQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotient(new ByteValue((byte) 25), new ByteValue((byte) 12)));
    }

    @Test
    public void testByteValueMin() {
        assertEquals(new ByteValue((byte) 1),
            instance.testMin(new ByteValue[] { new ByteValue("10"), new ByteValue("15"), new ByteValue("1") }));
    }

    @Test
    public void testBigDecimalValueAvg() {
        assertEquals(new BigDecimalValue("12.66667"),
            instance.testAvg(new BigDecimalValue[] { new BigDecimalValue("10"),
                    new BigDecimalValue("15"),
                    new BigDecimalValue("13") }));

    }

    @Test
    public void testOrCallingFromRules() {
        assertTrue(instance.checkOr());
    }

    @Test
    public void testXOR3arguments() {
        assertFalse(callXor(false, false, false));

        assertTrue(callXor(true, false, false));

        assertTrue(callXor(false, true, false));

        assertFalse(callXor(true, true, false));

        assertTrue(callXor(false, false, true));

        assertFalse(callXor(true, false, true));

        assertFalse(callXor(false, true, true));

        assertTrue(callXor(true, true, true));
    }

    @Test
    public void testXOR2arguments() {
        assertFalse(callXor(false, false));

        assertTrue(callXor(false, true));

        assertTrue(callXor(true, false));

        assertFalse(callXor(true, true));
    }

    @Test
    public void testOR2arguments() {
        assertFalse(callOr(false, false));

        assertTrue(callOr(false, true));

        assertTrue(callOr(true, false));

        assertTrue(callOr(true, true));
    }

    @Test
    public void testOR3arguments() {
        assertFalse(callOr(false, false, false));

        assertTrue(callOr(true, false, false));

        assertTrue(callOr(false, true, false));

        assertTrue(callOr(true, true, false));

        assertTrue(callOr(false, false, true));

        assertTrue(callOr(true, false, true));

        assertTrue(callOr(false, true, true));

        assertTrue(callOr(true, true, true));
    }

    @Test
    public void testRoundToLong() {
        assertEquals(1, RulesUtils.round(1.222235345345));

        assertEquals(2, RulesUtils.round(1.500000001235345345));

        assertEquals(0, RulesUtils.round(0));
    }

    @Test
    public void testRoundWithPrecision() {
        assertEquals("1.222", String.valueOf(RulesUtils.round(1.222235345345, 3)));

        assertEquals("1.6", String.valueOf(RulesUtils.round(1.56000001235345345, 1)));

        assertEquals("0.0", String.valueOf(RulesUtils.round(0, 0)));
    }

    private boolean callXor(boolean... values) {
        return RulesUtils.xor(values);
    }

    private boolean callOr(boolean... values) {
        return RulesUtils.anyTrue(values);
    }

    /* Tests for testing isEmpty methods */
    @Test
    public void testObjectEmptyArray() {
        Object[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new Object[2];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testByteEmptyArray() {
        byte[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new byte[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testCharEmptyArray() {
        char[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new char[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testShortEmptyArray() {
        short[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new short[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testIntEmptyArray() {
        int[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new int[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testLongEmptyArray() {
        long[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new long[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testFloatEmptyArray() {
        float[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new float[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testDoubleEmptyArray() {
        double[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new double[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testDateEmptyArray() {
        Date[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new Date[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testBigDecimalEmptyArray() {
        BigDecimal[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new BigDecimal[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testBigIntegerEmptyArray() {
        BigInteger[] array = null;
        assertTrue(RulesUtils.isEmpty(array));

        array = new BigInteger[5];
        assertFalse(RulesUtils.isEmpty(array));
    }

    @Test
    public void testEmptyString() {
        String str = null;
        assertTrue(RulesUtils.isEmpty(str));

        str = "";
        assertTrue(RulesUtils.isEmpty(str));

        str = " ";
        assertTrue(RulesUtils.isEmpty(str));

        str = "  str  ";
        assertFalse(RulesUtils.isEmpty(str));

        str = "str";
        assertFalse(RulesUtils.isEmpty(str));
    }

    @Test
    public void testStartsWith() {

        String prefix = "Test";
        assertTrue(RulesUtils.startsWith(str, prefix));

        String str2 = null;
        assertFalse(RulesUtils.startsWith(str2, prefix));
    }

    @Test
    public void testSubString() {
        int beginIndex = 3;
        int endIndex = 5;

        assertEquals("ting string value", RulesUtils.substring(str, beginIndex));
        assertEquals("ti", RulesUtils.substring(str, beginIndex, endIndex));
        assertEquals("", RulesUtils.substring("", beginIndex));
        assertEquals(null, RulesUtils.substring(null, 0));

    }

    @Test
    public void testRemoveStart() {
        String remove = "Testing";

        assertEquals(" string value", RulesUtils.removeStart(str, remove));
        assertEquals(null, RulesUtils.removeStart(null, remove));
        assertEquals("", RulesUtils.removeStart("", remove));
    }

    @Test
    public void testRemoveEnd() {
        String remove = "value";

        assertEquals("Testing string ", RulesUtils.removeEnd(str, remove));
        assertEquals(null, RulesUtils.removeEnd(null, remove));
        assertEquals("", RulesUtils.removeEnd("", remove));
    }

    @Test
    public void testStringCase() {
        String str = "Testing";

        assertEquals("TESTING", RulesUtils.upperCase(str));
        assertEquals("testing", RulesUtils.lowerCase(str));
        assertEquals(null, RulesUtils.upperCase(null));
        assertEquals("", RulesUtils.upperCase(""));
    }

    @Test
    public void testContainsString() {
        String searchString = "string";
        char searchChar = 's';

        assertTrue(RulesUtils.contains(str, searchString));
        assertTrue(RulesUtils.contains(str, searchChar));
        assertFalse(RulesUtils.contains(nullStr, searchChar));
        assertFalse(RulesUtils.contains("", searchChar));
    }

    @Test
    public void testContainsAny() {
        char[] searchChars = { 's', 'i', 'g' };
        String searchStr = "value";

        assertTrue(RulesUtils.containsAny(str, searchChars));
        assertTrue(RulesUtils.containsAny(str, searchStr));
        assertFalse(RulesUtils.containsAny(nullStr, searchStr));
        assertFalse(RulesUtils.containsAny("", searchStr));
    }

    @Test
    public void testReplace() {
        String text = "value Teting value string value";

        assertEquals("Testing string text", RulesUtils.replace(str, "value", "text"));
        assertEquals("text Teting text string text", RulesUtils.replace(text, "value", "text", 3));
        assertEquals(null, RulesUtils.replace(null, "value", "text"));
        assertEquals("", RulesUtils.replace("", "value", "text"));
    }

    @Test
    public void testStringSort() {

        String[] nullArray = null;
        String[] strValueArray = { null, "asd", "ac", null, null };
        String[] expecteds = { "ac", "asd", null, null, null };
        String[] actuals = RulesUtils.sort(strValueArray);

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expecteds, actuals);

    }

    @Test
    public void testDateSort() {
        
        int year = 2013;
        int month = 1;
        int date = 25;
        int hour = 15;
        int min = 3;
        Calendar c = Calendar.getInstance();
        Locale.setDefault(Locale.ENGLISH);
               
        c.set(year, month, date, hour, min);

        Date[] nullDateArray = null;
        Date[] nullDateArrayValue = { null, c.getTime(), c.getTime() };
        Date[] actuals = RulesUtils.sort(nullDateArrayValue);
        Date[] expecteds = {c.getTime(), c.getTime(), null };
        
        assertNull(RulesUtils.sort(nullDateArray));
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testDoubleSort() {
        double[] doubleArray = { 1.0, 2.0, 0.2 };
        double[] nullDoubleArray = null;
        double[] expected = { 0.2, 1.0, 2.0 };

        assertNull(RulesUtils.sort(nullDoubleArray));
        assertArrayEquals(expected, RulesUtils.sort(doubleArray), 0);
    }

    @Test
    public void testStringValueSort() {
        StringValue[] strValueArray = { null, new StringValue("asd"), new StringValue("ac"), null, null };
        StringValue[] expecteds = { new StringValue("ac"), new StringValue("asd"), null, null, null };
        StringValue[] actuals = RulesUtils.sort(strValueArray);

        assertArrayEquals(expecteds, actuals);

    }

    @Test
    public void testObjectValueSort() {
        ObjectValue[] strValueArray = { null, new ObjectValue("asd"), new ObjectValue("ac"), null, null };
        ObjectValue[] expecteds = { new ObjectValue("ac"), new ObjectValue("asd"), null, null, null };
        ObjectValue[] actuals = RulesUtils.sort(strValueArray);

        assertArrayEquals(expecteds, actuals);
    }
    
    @SuppressWarnings("deprecation")
    @Test   
    public void testDateFormat() {
        int year = 2013;
        int month = 1;
        int date = 25;
        int hour = 15;
        int min = 3;
        Calendar c = Calendar.getInstance();
        Locale.setDefault(Locale.ENGLISH);
               
        c.set(year, month, date, hour, min);
        
        System.out.println("Default locale is: " + Locale.getDefault());
        System.out.println("Locale date format: " + RulesUtils.dateToString(c.getTime()));
        
        assertEquals("2/25/13", RulesUtils.format(c.getTime()) );
        assertEquals("2/25/13", RulesUtils.dateToString(c.getTime()));
        
        assertEquals("25/13", RulesUtils.format(c.getTime(), "dd/YY"));
        assertEquals("25/13", RulesUtils.dateToString(c.getTime(), "dd/YY") );
        
        assertEquals("25/13 15:03", RulesUtils.format(c.getTime(), "dd/YY HH:mm"));
        assertEquals("25/13 15:03", RulesUtils.dateToString(c.getTime(), "dd/YY HH:mm"));
        
    }
}
