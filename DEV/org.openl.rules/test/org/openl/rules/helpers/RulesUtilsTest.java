package org.openl.rules.helpers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ObjectValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.rules.TestHelper;
import org.openl.util.ArrayTool;

/**
 * Test to check that methods from {@link RulesUtils} and children of
 * {@link org.openl.meta.explanation.ExplanationNumberValue} are visible and executed from excel.
 *
 * @author DLiauchuk
 *         <p/>
 *         TODO: test all methods
 */
public class RulesUtilsTest {

    private static final String SRC = "test/rules/helpers/RulesUtilsTest.xlsx";

    private static TestInterf instance;
    private static String str;

    public interface TestInterf {
        String testMaxByte(java.lang.Byte[] obj);

        String testMaxShort(java.lang.Short[] obj);

        String testMaxInt(java.lang.Integer[] obj);

        String testMaxLong(java.lang.Long[] onj);

        String testMaxFloat(java.lang.Float[] obj);

        String testMaxByteType(byte[] obj);

        String testMaxShortType(short[] obj);

        String testMaxIntType(int[] obj);

        String testMaxLongType(long[] onj);

        String testMaxFloatType(float[] obj);

        String testMaxBigInteger(BigInteger[] obj);

        String testMaxBigDecimal(BigDecimal[] obj);

        String testMinByte(java.lang.Byte[] obj);

        String testMinShort(java.lang.Short[] obj);

        String testMinInt(java.lang.Integer[] obj);

        String testMinLong(java.lang.Long[] onj);

        String testMinFloat(java.lang.Float[] obj);

        String testMinByteType(byte[] obj);

        String testMinShortType(short[] obj);

        String testMinIntType(int[] obj);

        String testMinLongType(long[] onj);

        String testMinFloatType(float[] obj);

        String testMinBigInteger(BigInteger[] obj);

        String testMinBigDecimal(BigDecimal[] obj);

        BigInteger testSumBigInteger(BigInteger[] values);

        BigDecimal testSumBigDecimal(BigDecimal[] values);

        java.lang.Byte testSumByte(java.lang.Byte[] values);

        java.lang.Short testSumShort(java.lang.Short[] values);

        java.lang.Integer testSumInteger(java.lang.Integer[] values);

        java.lang.Long testSumLong(java.lang.Long[] values);

        java.lang.Float testSumFloat(java.lang.Float[] values);

        byte testSumByteType(byte[] values);

        short testSumShortType(short[] values);

        int testSumIntegerType(int[] values);

        long testSumLongType(long[] values);

        float testSumFloatType(float[] values);

        java.lang.Double testAvgByte(java.lang.Byte[] values);

        java.lang.Double testAvgShort(java.lang.Short[] values);

        java.lang.Double testAvgInteger(java.lang.Integer[] values);

        java.lang.Double testAvgLong(java.lang.Long[] values);

        java.lang.Float testAvgFloat(java.lang.Float[] values);

        java.lang.Double testAvgDouble(java.lang.Double[] values);

        Double testAvgByteType(byte[] values);

        Double testAvgShortType(short[] values);

        Double testAvgIntegerType(int[] values);

        Double testAvgLongType(long[] values);

        Float testAvgFloatType(float[] values);

        Double testAvgDoubleType(double[] values);

        BigDecimalValue testAvgBigDecimal(BigDecimalValue[] values);

        BigDecimalValue testAvgBigInteger(BigIntegerValue[] values);

        LongValue testQuaotientByteValue(ByteValue number, ByteValue divisor);

        LongValue testQuaotientShortValue(ShortValue number, ShortValue divisor);

        LongValue testQuaotientIntegerValue(IntValue number, IntValue divisor);

        LongValue testQuaotientLongValue(LongValue number, LongValue divisor);

        LongValue testQuaotientFloatValue(FloatValue number, FloatValue divisor);

        LongValue testQuaotientDoubleValue(DoubleValue number, DoubleValue divisor);

        LongValue testQuaotientBigIntegerValue(BigIntegerValue number, BigIntegerValue divisor);

        LongValue testQuaotientBigDecimalValue(BigDecimalValue number, BigDecimalValue divisor);

        LongValue testQuaotientByte(Byte number, Byte divisor);

        LongValue testQuaotientShort(Short number, Short divisor);

        LongValue testQuaotientInteger(Integer number, Integer divisor);

        LongValue testQuaotientLong(Long number, Long divisor);

        LongValue testQuaotientFloat(Float number, Float divisor);

        LongValue testQuaotientDouble(Double number, Double divisor);

        LongValue testQuaotientBigInteger(BigInteger number, BigInteger divisor);

        LongValue testQuaotientBigDecimal(BigDecimal number, BigDecimal divisor);

        LongValue testQuaotientByteType(byte number, byte divisor);

        LongValue testQuaotientShortType(short number, short divisor);

        LongValue testQuaotientIntegerType(int number, int divisor);

        LongValue testQuaotientLongType(long number, long divisor);

        LongValue testQuaotientFloatType(float number, float divisor);

        LongValue testQuaotientDoubleType(double number, double divisor);

        ByteValue testMinByteValue(ByteValue[] byteValues);

        ShortValue testMinShortValue(ShortValue[] shortValues);

        IntValue testMinIntegerValue(IntValue[] intValues);

        LongValue testMinLongValue(LongValue[] longValues);

        FloatValue testMinFloatValue(FloatValue[] floatValues);

        DoubleValue testMinDoubleValue(DoubleValue[] doubleValues);

        BigIntegerValue testMinBigIntegerValue(BigIntegerValue[] bigIntegerValues);

        BigDecimalValue testMinBigDecimalValue(BigDecimalValue[] bigDecimalValues);

        ByteValue testMaxByteValue(ByteValue[] byteValues);

        ShortValue testMaxShortValue(ShortValue[] shortValues);

        IntValue testMaxIntegerValue(IntValue[] intValues);

        LongValue testMaxLongValue(LongValue[] longValues);

        FloatValue testMaxFloatValue(FloatValue[] floatValues);

        DoubleValue testMaxDoubleValue(DoubleValue[] doubleValues);

        BigIntegerValue testMaxBigIntegerValue(BigIntegerValue[] bigIntegerValues);

        BigDecimalValue testMaxBigDecimalValue(BigDecimalValue[] bigDecimalValues);

        ByteValue testModByteValue(ByteValue byteValue, ByteValue byteValue2);

        ShortValue testModShortValue(ShortValue shortValue, ShortValue shortValue2);

        IntValue testModIntegerValue(IntValue intValue, IntValue intValue2);

        LongValue testModLongValue(LongValue longValue, LongValue longValue2);

        FloatValue testModFloatValue(FloatValue floatValue, FloatValue floatValue2);

        BigDecimalValue testModBigDecimalValue(BigDecimalValue bigDecimalValue, BigDecimalValue bigDecimalValue2);

        BigIntegerValue testModBigIntegerValue(BigIntegerValue bigIntegerValue, BigIntegerValue bigIntegerValue2);

        Byte testModByte(Byte byte1, Byte byte2);

        Short testModShort(Short short1, Short short2);

        Integer testModInteger(Integer integer, Integer integer2);

        Long testModLong(Long long1, Long long2);

        Float testModFloat(Float float1, Float float2);

        BigDecimal testModBigDecimal(BigDecimal bigDecimal, BigDecimal bigDecimal2);

        BigInteger testModBigInteger(BigInteger valueOf, BigInteger valueOf2);

        byte testModByteType(byte b, byte c);

        short testModShortType(short s, short t);

        int testModIntegerType(int i, int j);

        long testModLongType(long l, long m);

        float testModFloatType(float f, float g);

        boolean checkOr();

        Byte[] testSliceByte(Byte[] byteValues, int startIndex);

        Byte[] testSliceByte(Byte[] bytes, int i, int j);

        Short[] testSliceShort(Short[] shorts, int i, int j);

        Short[] testSliceShort(Short[] shorts, int i);

        Integer[] testSliceInteger(Integer[] integers, int i);

        Integer[] testSliceInteger(Integer[] integers, int i, int j);

        Long[] testSliceLong(Long[] longs, int i);

        Long[] testSliceLong(Long[] longs, int i, int j);

        Float[] testSliceFloat(Float[] floats, int i);

        Float[] testSliceFloat(Float[] floats, int i, int j);

        Double[] testSliceDouble(Double[] doubles, int i, int j);

        Double[] testSliceDouble(Double[] doubles, int i);

        BigInteger[] testSliceBigInteger(BigInteger[] bigIntegers, int i);

        BigInteger[] testSliceBigInteger(BigInteger[] bigIntegers, int i, int j);

        BigDecimal[] testSliceBigDecimal(BigDecimal[] bigDecimals, int i);

        BigDecimal[] testSliceBigDecimal(BigDecimal[] bigDecimals, int i, int j);

        byte[] testSliceByteType(byte[] bs, int i);

        byte[] testSliceByteType(byte[] bs, int i, int j);

        short[] testSliceShortType(short[] s, int i);

        short[] testSliceShortType(short[] s, int i, int j);

        int[] testSliceIntegerType(int[] is, int i);

        int[] testSliceIntegerType(int[] is, int i, int j);

        long[] testSliceLongType(long[] ls, int i);

        long[] testSliceLongType(long[] ls, int i, int j);

        float[] testSliceFloatType(float[] fs, int i);

        float[] testSliceFloatType(float[] fs, int i, int j);

        double[] testSliceDoubleType(double[] ds, int i);

        double[] testSliceDoubleType(double[] ds, int i, int j);

        ObjectValue[] testSortObjectValue(ObjectValue[] strValueArray);

        Date[] testSortDate(Date[] nullDateArrayValue);

        StringValue[] testSortStringValue(StringValue[] strValueArray);

        String[] testSortString(String[] strValueArray);

        BigDecimalValue[] testSortBigDecimalValue(BigDecimalValue[] inputArray);

        BigIntegerValue[] testSortBigIntegerValue(BigIntegerValue[] inputArray);

        BigDecimal[] testSortBigDecimal(BigDecimal[] inputArray);

        BigInteger[] testSortBigInteger(BigInteger[] inputArray);

        DoubleValue[] testSortDoubleValue(DoubleValue[] inputArray);

        FloatValue[] testSortFloatValue(FloatValue[] inputArray);

        LongValue[] testSortLongValue(LongValue[] inputArray);

        IntValue[] testSortIntegerValue(IntValue[] inputArray);

        ShortValue[] testSortShortValue(ShortValue[] inputArray);

        ByteValue[] testSortByteValue(ByteValue[] inputArray);

        Double[] testSortDouble(Double[] inputArray);

        Float[] testSortFloat(Float[] inputArray);

        Long[] testSortLong(Long[] inputArray);

        Integer[] testSortInteger(Integer[] inputArray);

        Short[] testSortShort(Short[] inputArray);

        Byte[] testSortByte(Byte[] inputArray);

        double[] testSortDoubleType(double[] inputArray);

        float[] testSortFloatType(float[] inputArray);

        long[] testSortLongType(long[] inputArray);

        int[] testSortIntegerType(int[] inputArray);

        short[] testSortShortType(short[] inputArray);

        byte[] testSortByteType(byte[] inputArray);

        boolean testContainsCharTypeArrInCharArr(char[] searchIn, char[] searchFor);

        boolean testContainsBoolTypeArrInBoolArr(boolean[] searchIn, boolean[] searchFor);

        boolean testContainsDoubleTypeArrInDoubleArr(double[] searchIn, double[] searchFor);

        boolean testContainsFloatTypeArrInFloatArr(float[] searchIn, float[] searchFor);

        boolean testContainsShortTypeArrInShortArr(short[] searchIn, short[] searchFor);

        boolean testContainsByteTypeArrInByteArr(byte[] searchIn, byte[] searchFor);

        boolean testContainsLongTypeArrInLongArr(long[] searchIn, long[] searchFor);

        boolean testContainsIntegerTypeArrInIntegerArr(int[] searchIn, int[] searchFor);

        boolean testContainsObjectArrInObjectArr(Object[] searchIn, Object[] searchFor);

        boolean testContainsStringArrInStringArr(String[] searchIn, String[] searchFor);

        boolean testContainsCharArrInCharArr(Character[] searchIn, Character[] searchFor);

        boolean testContainsBoolArrInBoolArr(Boolean[] searchIn, Boolean[] searchFor);

        boolean testContainsDoubleArrInDoubleArr(Double[] searchIn, Double[] searchFor);

        boolean testContainsFloatArrInFloatArr(Float[] searchIn, Float[] searchFor);

        boolean testContainsShortArrInShortArr(Short[] searchIn, Short[] searchFor);

        boolean testContainsByteArrInByteArr(Byte[] searchIn, Byte[] searchFor);

        boolean testContainsLongArrInLongArr(Long[] searchIn, Long[] searchFor);

        boolean testContainsIntegerArrInIntegerArr(Integer[] searchIn, Integer[] searchFor);

        boolean testContainsBoolTypeInBoolArr(boolean[] searchIn, boolean searchFor);

        boolean testContainsDoubleTypeInDoubleArr(double[] searchIn, double searchFor);

        boolean testContainsFloatTypeInFloatArr(float[] searchIn, float searchFor);

        boolean testContainsCharTypeInCharArr(char[] searchIn, char searchFor);

        boolean testContainsShortTypeInShortArr(short[] searchIn, short searchFor);

        boolean testContainsByteTypeInByteArr(byte[] searchIn, byte searchFor);

        boolean testContainsLongTypeInLongArr(long[] searchIn, long searchFor);

        boolean testContainsIntegerTypeInIntegerArr(int[] searchIn, int searchFor);

        boolean testContainsObjectInObjectArr(Object[] searchIn, Object searchFor);

        boolean testContainsBoolInBoolArr(Boolean[] searchIn, Boolean searchFor);

        boolean testContainsDoubleInDoubleArr(Double[] searchIn, Double searchFor);

        boolean testContainsFloatInFloatArr(Float[] searchIn, Float searchFor);

        boolean testContainsCharInCharArr(Character[] searchIn, Character searchFor);

        boolean testContainsShortInShortArr(Short[] searchIn, Short searchFor);

        boolean testContainsByteInByteArr(Byte[] searchIn, Byte searchFor);

        boolean testContainsLongInLongArr(Long[] searchIn, Long searchFor);

        boolean testContainsIntegerInIntegerArr(Integer[] searchIn, Integer searchFor);

        Object testIndexOfObject(Object[] objects, Object object);

        Object testIndexOfBoolType(boolean[] bs, boolean b);

        Object testIndexOfDoubleType(double[] ds, double d);

        Object testIndexOfFloatType(float[] fs, float f);

        Object testIndexOfCharType(char[] cs, char c);

        Object testIndexOfShortType(short[] s, short t);

        Object testIndexOfByteType(byte[] bs, byte b);

        Object testIndexOfLongType(long[] ls, long l);

        Object testIndexOfIntegerType(int[] is, int i);

        Object testIndexOfBool(Boolean[] bs, Boolean b);

        Object testIndexOfDouble(Double[] ds, Double d);

        Object testIndexOfFloat(Float[] fs, Float f);

        Object testIndexOfChar(Character[] cs, Character c);

        Object testIndexOfShort(Short[] s, Short t);

        Object testIndexOfByte(Byte[] bs, Byte b);

        Object testIndexOfLong(Long[] ls, Long l);

        Object testIndexOfInteger(Integer[] is, Integer i);

        boolean testNoNullsObject(Object[] objects);

        void testError(String string);

        Object formatDouble(double d);

        Object formatDoubleWithFrm(double d, String string);

        Object[] testIntersectionStringArr(String[] searchIn, String[] searchFor);

        Object testAbsMonth(Date dateNow);

        Object testAbsQuarter(Date dateNow);

        Object testDiffDate(Date date1, Date date2);

        Object testDayOfMonth(Date date1);

        Date testFirstDayOfQuarter(int i);

        Object testLastDayOfQuarter(int i);

        Object testLastDayOfMonth(Date time);

        Object testGetMonth(Date time);

        int testMonthDiff(Date date1, Date date2);

        Object testYearDiff(Date endDate, Date startDate);

        Object testWeekDiff(Date endDate, Date startDate);

        Object testQuarter(Date date);

        Object testYear(Date date);

        Object testDayOfWeek(Date date);

        Object testDayOfYear(Date date);

        Object testWeekOfYear(Date date);

        Object testWeekOfMonth(Date date);

        Object testSecond(Date date);

        Object testMinute(Date date);

        Object testHour(Date date);

        Object testHourOfDay(Date date);

        Object testAmPm(Date date);

        Object testDoubleProduct(Double[] inputArray);

        double testFloatProduct(Float[] inputArray);

        Object testLongProduct(Long[] inputArray);

        Object testIntegerProduct(Integer[] inputArray);

        Object testBigDecimalProduct(BigDecimal[] inputArray);

        Object testBigIntegerProduct(BigInteger[] inputArray);

        Object testShortProduct(Short[] inputArray);

        Object testByteProduct(Byte[] inputArray);

        Object testDoubleTypeProduct(double[] inputArray);

        double testFloatTypeProduct(float[] inputArray);

        Object testLongTypeProduct(long[] inputArray);

        Object testIntegerTypeProduct(int[] inputArray);

        Object testShortTypeProduct(short[] inputArray);

        Object testByteTypeProduct(byte[] inputArray);

        Object testDoubleTypeRound(double d);

        Object testFloatTypeRound(float f);

        Object testDoubleTypeRoundScale(double d, int i);

        Object testFloatTypeRoundScale(float d, int i);

        Object testDoubleTypeRoundScaleRoundMethod(double d, int i, int j);

        Object testFloatTypeRoundScaleRoundMethod(float f, int i, int j);

        Object testBigDecimalRound(BigDecimal valueOf);

        Object testBigDecimalScaleRound(BigDecimal valueOf, int i);

        Object testBigDecimalScaleRoundRoundMethod(BigDecimal valueOf, int i, int j);

        Object[] testRemoveNulls(Integer[] inputArray);

        Object testIntegerAbs(int i);

        Object testLongAbs(long l);

        Object testFloatAbs(float f);

        Object testDoubleAbs(double d);

        Object testAcos(double d);

        Object testAsin(double d);

        Object testAtan(double d);

        Object testAtan2(double d, double e);

        Object testCbrt(double d);

        Object testCeil(double d);

        Object testDoubleCopySign(double d, double e);

        Object testFloatCopySign(float f, float g);

        Object testCos(double d);

        Object testCosh(double d);

        Object testExp(double d);

        Object testExpm1(double d);

        Object testBigDecimalMax(BigDecimal valueOf, BigDecimal valueOf2);

        Object testBigIntegerMax(BigInteger valueOf, BigInteger valueOf2);

        Object testLongMax(Long valueOf, Long valueOf2);

        Object testDoubleMax(Double valueOf, Double valueOf2);

        Object testFloatMax(Float valueOf, Float valueOf2);

        Object testIntegerMax(Integer valueOf, Integer valueOf2);

        Byte testByteMax(Byte valueOf, Byte valueOf2);

        Short testShortMax(Short valueOf, Short valueOf2);

        Object testLongTypeMax(long l, long m);

        Object testDoubleTypeMax(double d, double e);

        Object testFloatTypeMax(float f, float g);

        Object testIntegerTypeMax(int i, int j);

        Object testBigDecimalMin(BigDecimal valueOf, BigDecimal valueOf2);

        Object testBigIntegerMin(BigInteger valueOf, BigInteger valueOf2);

        Object testLongMin(Long valueOf, Long valueOf2);

        Object testDoubleMin(Double valueOf, Double valueOf2);

        Object testFloatMin(Float valueOf, Float valueOf2);

        Object testIntegerMin(Integer valueOf, Integer valueOf2);

        Byte testByteMin(Byte valueOf, Byte valueOf2);

        Short testShortMin(Short valueOf, Short valueOf2);

        Object testLongTypeMin(long l, long m);

        Object testDoubleTypeMin(double d, double e);

        Object testFloatTypeMin(float f, float g);

        Object testIntegerTypeMin(int i, int j);

        Object testToDegrees(double d);

        Object testToRadians(double d);

        boolean[] testBooleanTypeAdd(boolean[] inputArray, boolean b);

        boolean[] testBooleanTypeAdd(boolean[] inputArray, int i, boolean b);

        Boolean[] testBooleanAdd(Boolean[] inputArray, Boolean b);

        Boolean[] testBooleanAdd(Boolean[] inputArray, int i, Boolean b);

        double[] testDoubleTypeAdd(double[] inputArray, double d);

        double[] testDoubleTypeAdd(double[] inputArray, int d, double e);

        Double[] testDoubleAdd(Double[] inputArray, Double d);

        Double[] testDoubleAdd(Double[] inputArray, int d, Double e);

        short[] testShortTypeAdd(short[] inputArray, int i, short j);

        short[] testShortTypeAdd(short[] inputArray, short i);

        Short[] testShortAdd(Short[] inputArray, int i, Short j);

        Short[] testShortAdd(Short[] inputArray, Short i);

        Object[] testObjectTypeAdd(Object[] inputArray, int i, ObjectValue objectValue);

        Object[] testObjectTypeAdd(Object[] inputArray, ObjectValue i);

        long[] testLongTypeAdd(long[] inputArray, int i, long j);

        long[] testLongTypeAdd(long[] inputArray, long i);

        Long[] testLongAdd(Long[] inputArray, int i, Long j);

        Long[] testLongAdd(Long[] inputArray, Long i);

        int[] testIntegerTypeAdd(int[] inputArray, int i, int j);

        int[] testIntegerTypeAdd(int[] inputArray, int i);

        Integer[] testIntegerAdd(Integer[] inputArray, int i, Integer j);

        Integer[] testIntegerAdd(Integer[] inputArray, Integer i);

        float[] testFloatTypeAdd(float[] inputArray, int i, float f);

        float[] testFloatTypeAdd(float[] inputArray, float f);

        Float[] testFloatAdd(Float[] inputArray, int i, Float f);

        Float[] testFloatAdd(Float[] inputArray, Float f);

        char[] testCharTypeAdd(char[] inputArray, int i, char c);

        char[] testCharTypeAdd(char[] inputArray, char c);

        Character[] testCharAdd(Character[] inputArray, int i, Character c);

        Character[] testCharAdd(Character[] inputArray, Character c);

        byte[] testByteTypeAdd(byte[] inputArray, int i, byte j);

        byte[] testByteTypeAdd(byte[] inputArray, byte i);

        Byte[] testByteAdd(Byte[] inputArray, int i, Byte j);

        Byte[] testByteAdd(Byte[] inputArray, Byte i);

        Object[] testObjectTypeAddIgnoreNulls(Object[] inputArray, ObjectValue objectValue);

        Object[] testObjectTypeAddIgnoreNulls(Object[] inputArray, int i, ObjectValue objectValue);

        Object[] testObjectAddIgnoreNulls(Object[] inputArray, Object objectValue);

        Object[] testObjectAddIgnoreNulls(Object[] inputArray, int i, Object objectValue);

        byte[] testByteTypeAddAll(byte[] inputArray1, byte[] inputArray2);

        boolean[] testBooleanTypeAddAll(boolean[] inputArray1, boolean[] inputArray2);

        char[] testCharTypeAddAll(char[] inputArray1, char[] inputArray2);

        double[] testDoubleTypeAddAll(double[] inputArray2, double[] inputArray1);

        float[] testFloatTypeAddAll(float[] inputArray1, float[] inputArray2);

        int[] testIntegerTypeAddAll(int[] inputArray1, int[] inputArray2);

        long[] testLongTypeAddAll(long[] inputArray2, long[] inputArray1);

        Object[] testObjectAddAll(Object[] inputArray2, Object[] inputArray1);

        short[] testShortTypeAddAll(short[] inputArray2, short[] inputArray1);

        Byte[] testByteAddAll(Byte[] inputArray1, Byte[] inputArray2);

        Boolean[] testBooleanAddAll(Boolean[] inputArray1, Boolean[] inputArray2);

        Character[] testCharAddAll(Character[] inputArray1, Character[] inputArray2);

        Double[] testDoubleAddAll(Double[] inputArray2, Double[] inputArray1);

        Float[] testFloatAddAll(Float[] inputArray1, Float[] inputArray2);

        Integer[] testIntegerAddAll(Integer[] inputArray1, Integer[] inputArray2);

        Long[] testLongAddAll(Long[] inputArray2, Long[] inputArray1);

        Short[] testShortAddAll(Short[] inputArray2, Short[] inputArray1);

        boolean[] testBooleanTypeRemove(boolean[] inputArray1, int i);

        short[] testShortTypeRemove(short[] inputArray1, int i);

        Object[] testObjectTypeRemove(Object[] inputArray1, int i);

        long[] testLongTypeRemove(long[] inputArray1, int i);

        int[] testIntegerTypeRemove(int[] inputArray1, int i);

        float[] testFloatTypeRemove(float[] inputArray1, int i);

        double[] testDoubleTypeRemove(double[] inputArray1, int i);

        char[] testCharTypeRemove(char[] inputArray1, int i);

        byte[] testByteTypeRemove(byte[] inputArray1, int i);

        Boolean[] testBooleanRemove(Boolean[] inputArray1, int i);

        Short[] testShortRemove(Short[] inputArray1, int i);

        Long[] testLongRemove(Long[] inputArray1, int i);

        Integer[] testIntegerRemove(Integer[] inputArray1, int i);

        Float[] testFloatRemove(Float[] inputArray1, int i);

        Double[] testDoubleRemove(Double[] inputArray1, int i);

        Character[] testCharRemove(Character[] inputArray1, int i);

        Byte[] testByteRemove(Byte[] inputArray1, int i);

        boolean[] testBooleanTypeRemoveElement(boolean[] inputArray1, boolean b);

        short[] testShortTypeRemoveElement(short[] inputArray1, short i);

        Object[] testObjectTypeRemoveElement(Object[] inputArray1, Object i);

        long[] testLongTypeRemoveElement(long[] inputArray1, long i);

        int[] testIntegerTypeRemoveElement(int[] inputArray1, int i);

        float[] testFloatTypeRemoveElement(float[] inputArray1, float f);

        double[] testDoubleTypeRemoveElement(double[] inputArray1, double d);

        char[] testCharTypeRemoveElement(char[] inputArray1, char c);

        byte[] testByteTypeRemoveElement(byte[] inputArray1, byte i);

        Boolean[] testBooleanRemoveElement(Boolean[] inputArray1, Boolean b);

        Short[] testShortRemoveElement(Short[] inputArray1, Short i);

        Long[] testLongRemoveElement(Long[] inputArray1, Long i);

        Integer[] testIntegerRemoveElement(Integer[] inputArray1, Integer i);

        Float[] testFloatRemoveElement(Float[] inputArray1, Float f);

        Double[] testDoubleRemoveElement(Double[] inputArray1, Double d);

        Character[] testCharRemoveElement(Character[] inputArray1, Character c);

        Byte[] testByteRemoveElement(Byte[] inputArray1, Byte i);

        boolean testStringIsEmpty(String[] inputArray);

        boolean testBigIntegerIsEmpty(BigInteger[] inputArray);

        boolean testBigDecimalIsEmpty(BigDecimal[] inputArray);

        boolean testDateIsEmpty(Date[] inputArray);

        boolean testFloatTypeIsEmpty(float[] inputArray);

        boolean testLongTypeIsEmpty(long[] inputArray);

        boolean testIntegerTypeIsEmpty(int[] inputArray);

        boolean testShortTypeIsEmpty(short[] inputArray);

        boolean testCharTypeIsEmpty(char[] inputArray);

        boolean testByteTypeIsEmpty(byte[] inputArray);

        boolean testObjectIsEmpty(Object[] inputArray);

        String testRemoveStart(String object, String remove);

        Object testReplace(String string, String string2, String string3);

        Object testLowerCase(String string);

        Object testUpperCase(String object);

        Object testRemoveEnd(String string, String remove);

        Object testSubString(String object, int i);

        boolean testEndsWith(String str2, String prefix);

        Object testSubString(String str, int beginIndex, int endIndex);

        boolean testStartsWith(String str2, String prefix);

        Object testReplace(String text, String string, String string2, int i);

        Object testDoubleSmall(Double[] array, int i);

        Object testFloatSmall(Float[] array, int i);

        Long testLongSmall(Long[] array, int i);

        Integer testIntegerSmall(Integer[] array, int i);

        Short testShortSmall(Short[] array, int i);

        Byte testByteSmall(Byte[] array, int i);

        Object testBigIntegerSmall(BigInteger[] array, int i);

        Object testBigDecimalSmall(BigDecimal[] array, int i);

        double testDoubleTypeSmall(double[] array, int i);

        float testFloatTypeSmall(float[] array, int i);

        long testLongTypeSmall(long[] array, int i);

        int testIntegerTypeSmall(int[] array, int i);

        short testShortTypeSmall(short[] array, int i);

        byte testByteTypeSmall(byte[] array, int i);

        Double testDoubleBig(Double[] array, int i);

        Object testFloatBig(Float[] array, int i);

        Long testLongBig(Long[] array, int i);

        Integer testIntegerBig(Integer[] array, int i);

        Short testShortBig(Short[] array, int i);

        Byte testByteBig(Byte[] array, int i);

        Object testBigIntegerBig(BigInteger[] array, int i);

        Object testBigDecimalBig(BigDecimal[] array, int i);

        double testDoubleTypeBig(double[] array, int i);

        float testFloatTypeBig(float[] array, int i);

        long testLongTypeBig(long[] array, int i);

        int testIntegerTypeBig(int[] array, int i);

        short testShortTypeBig(short[] array, int i);

        byte testByteTypeBig(byte[] array, int i);

        Object[] testFlatten(Object... objects);

        Object[] testGetValuesAlias();

        Object[] testGetValuesPrimesAlias();

        boolean testInstanceOf(Long a);

        Double testMaxDouble(Double[] values);

        Double testMaxDoubleType(double[] values);

        Double testMinDouble(Double[] values);

        Double testMinDoubleType(double[] values);

        Double testSumDouble(Double[] values);

        Double testSumDoubleType(double[] values);

        BigDecimal testAvgBigIntegerArray(BigInteger[] values);

        BigDecimal testAvgBigDecimalArray(BigDecimal[] values);

        Double testMedianByte(Byte[] values);

        Double testMedianShort(Short[] values);

        Double testMedianInteger(Integer[] values);

        Double testMedianLong(Long[] values);

        Float testMedianFloat(Float[] values);

        Double testMedianDouble(Double[] values);

        Double testMedianByteType(byte[] values);

        Double testMedianShortType(short[] values);

        Double testMedianIntegerType(int[] values);

        Double testMedianLongType(long[] values);

        Float testMedianFloatType(float[] values);

        Double testMedianDoubleType(double[] values);

        BigDecimal testMedianBigInteger(BigInteger[] values);

        BigDecimal testMedianBigDecimal(BigDecimal[] values);

        Double testModDouble(Double number, Double divisor);

        double testModDoubleType(double number, double divisor);
    }

    @Before
    public void init() {
        Locale.setDefault(Locale.US);

        if (instance == null) {
            File xlsFile = new File(SRC);
            TestHelper<TestInterf> testHelper;
            testHelper = new TestHelper<TestInterf>(xlsFile, TestInterf.class);
            str = "Testing string value";

            instance = testHelper.getInstance();
        }
    }

    @Test
    public void testByteMax() {
        assertEquals("res2", instance.testMaxByte(new java.lang.Byte[] { (byte) 127, (byte) -128 }));
    }

    @Test
    public void testInstanceOf() {
        assertTrue(instance.testInstanceOf(new Long(1)));
    }

    @Test
    public void testShortMax() {
        assertEquals("res1", instance.testMaxShort(new java.lang.Short[] { (short) 32767, (short) -32768 }));
    }

    @Test
    public void testIntMax() {
        assertEquals("res1", instance.testMaxInt(new java.lang.Integer[] { (int) 2147483647, (int) -2147483648 }));
    }

    @Test
    public void testLongMax() {
        Long a = new Long("9200000000000000000");
        Long b = new Long("9100000000000000009");
        assertEquals("res1", instance.testMaxLong(new java.lang.Long[] { a, b }));
    }

    @Test
    public void testFloatMax() {
        Float a = new Float("3.40000000000000000000000000000000000000");
        Float b = new Float("2.40000000000000000000000000000000000000");
        assertEquals("res1", instance.testMaxFloat(new java.lang.Float[] { a, b }));
    }

    @Test
    public void testByteTypeMax() {
        assertEquals("res2", instance.testMaxByteType(new byte[] { (byte) 127, (byte) -128 }));
    }

    @Test
    public void testShortTypeMax() {
        assertEquals("res1", instance.testMaxShortType(new short[] { (short) 32767, (short) -32768 }));
    }

    @Test
    public void testIntTypeMax() {
        assertEquals("res1", instance.testMaxIntType(new int[] { (int) 2147483647, (int) -2147483648 }));
    }

    @Test
    public void testLongTypeMax() {
        Long a = new Long("9200000000000000000");
        Long b = new Long("9100000000000000009");
        assertEquals("res1", instance.testMaxLongType(new long[] { a, b }));
    }

    @Test
    public void testFloatTypeMax() {
        Float a = new Float("3.40000000000000000000000000000000000000");
        Float b = new Float("2.40000000000000000000000000000000000000");
        assertEquals("res1", instance.testMaxFloatType(new float[] { a, b }));
    }

    @Test
    public void testBigIntegerMax() {
        assertEquals("res1",
            instance.testMaxBigInteger(new BigInteger[] { BigInteger.valueOf(0), BigInteger.valueOf(123) }));
    }

    @Test
    public void testBigDecimalMax() {
        assertEquals("res1",
            instance.testMaxBigDecimal(new BigDecimal[] { BigDecimal.valueOf(0), BigDecimal.valueOf(123) }));
    }

    @Test
    public void testByteMin() {
        assertEquals("res1", instance.testMinByte(new java.lang.Byte[] { (byte) 127, (byte) -128 }));
    }

    @Test
    public void testShortMin() {
        assertEquals("res2", instance.testMinShort(new java.lang.Short[] { (short) 32767, (short) -32768 }));
    }

    @Test
    public void testIntMin() {
        assertEquals("res2", instance.testMinInt(new java.lang.Integer[] { (int) 2147483647, (int) -2147483648 }));
    }

    @Test
    public void testLongMin() {
        Long a = new Long("9200000000000000000");
        Long b = new Long("9100000000000000009");
        assertEquals("res2", instance.testMinLong(new java.lang.Long[] { a, b }));
    }

    @Test
    public void testFloatMin() {
        Float a = new Float("3.40000000000000000000000000000000000000");
        Float b = new Float("2.40000000000000000000000000000000000000");
        assertEquals("res2", instance.testMinFloat(new java.lang.Float[] { a, b }));
    }

    @Test
    public void testByteTypeMin() {
        assertEquals("res1", instance.testMinByteType(new byte[] { (byte) 127, (byte) -128 }));
    }

    @Test
    public void testShortTypeMin() {
        assertEquals("res2", instance.testMinShortType(new short[] { (short) 32767, (short) -32768 }));
    }

    @Test
    public void testIntTypeMin() {
        assertEquals("res2", instance.testMinIntType(new int[] { (int) 2147483647, (int) -2147483648 }));
    }

    @Test
    public void testLongTypeMin() {
        Long a = new Long("9200000000000000000");
        Long b = new Long("9100000000000000009");
        assertEquals("res2", instance.testMinLongType(new long[] { a, b }));
    }

    @Test
    public void testFloatTypeMin() {
        Float a = new Float("3.40000000000000000000000000000000000000");
        Float b = new Float("2.40000000000000000000000000000000000000");
        assertEquals("res2", instance.testMinFloatType(new float[] { a, b }));
    }

    @Test
    public void testBigIntegerMin() {
        assertEquals("res2",
            instance.testMinBigInteger(new BigInteger[] { BigInteger.valueOf(0), BigInteger.valueOf(123) }));
    }

    @Test
    public void testBigDecimalMin() {
        assertEquals("res2",
            instance.testMinBigDecimal(new BigDecimal[] { BigDecimal.valueOf(0), BigDecimal.valueOf(123) }));
    }

    @Test
    public void testByteSum() {
        assertEquals(java.lang.Byte.valueOf((byte) 127),
            instance.testSumByte(new java.lang.Byte[] { java.lang.Byte.valueOf((byte) 0),
                    java.lang.Byte.valueOf((byte) 101),
                    java.lang.Byte.valueOf((byte) 26) }));
    }

    @Test
    public void testShortSum() {
        assertEquals(java.lang.Short.valueOf((short) 30),
            instance.testSumShort(new java.lang.Short[] { java.lang.Short.valueOf((short) 10),
                    java.lang.Short.valueOf((short) 5),
                    java.lang.Short.valueOf((short) 15) }));
    }

    @Test
    public void testIntegerSum() {
        assertEquals((Integer) 30, instance.testSumInteger(new java.lang.Integer[] { 10, 5, 15 }));
    }

    @Test
    public void testLongSum() {
        assertEquals(java.lang.Long.valueOf((long) 30),
            instance.testSumLong(new java.lang.Long[] { java.lang.Long.valueOf((long) 10),
                    java.lang.Long.valueOf((long) 5),
                    java.lang.Long.valueOf((long) 15) }));
    }

    @Test
    public void testFloatSum() {
        assertEquals(java.lang.Float.valueOf((float) 30.5),
            instance.testSumFloat(new java.lang.Float[] { java.lang.Float.valueOf((float) 10.1),
                    java.lang.Float.valueOf((float) 5.3),
                    java.lang.Float.valueOf((float) 15.1) }),
            1e-15);
    }

    @Test
    public void testBigDecimalSum() {
        assertEquals(BigDecimal.valueOf(30),
            instance.testSumBigDecimal(
                new BigDecimal[] { BigDecimal.valueOf(10), BigDecimal.valueOf(5), BigDecimal.valueOf(15) }));
    }

    @Test
    public void testBigIntegerSum() {
        assertEquals(BigInteger.valueOf(30),
            instance.testSumBigInteger(
                new BigInteger[] { BigInteger.valueOf(10), BigInteger.valueOf(5), BigInteger.valueOf(15) }));
    }

    @Test
    public void testByteTypeSum() {
        assertEquals(((byte) 127), instance.testSumByteType(new byte[] { (byte) 101, (byte) 0, (byte) 26 }));
    }

    @Test
    public void testShortTypeSum() {
        assertEquals((short) 30, instance.testSumShortType(new short[] { (short) 10, (short) 5, (short) 15 }));
    }

    @Test
    public void testIntegerTypeSum() {
        assertEquals(30, instance.testSumIntegerType(new int[] { 10, 5, 15 }));
    }

    @Test
    public void testLongTypeSum() {
        assertEquals((long) 30, instance.testSumLongType(new long[] { (long) 10, (long) 5, (long) 15 }));
    }

    @Test
    public void testFloatTypeSum() {
        assertEquals((float) 30.5,
            instance.testSumFloatType(new float[] { (float) 10.3, (float) 5.1, (float) 15.1 }),
            1e-8);
    }

    @Test
    public void testByteTypeAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgByteType(new byte[] { (byte) 10, (byte) 15, (byte) 13 }),
            1e-8);
    }

    @Test
    public void testShortTypeAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgShortType(new short[] { (short) 10, (short) 15, (short) 13 }),
            1e-8);
    }

    @Test
    public void testIntegerTypeAvg() {
        assertEquals(12.666666666666666d, instance.testAvgIntegerType(new int[] { 10, 15, 13 }), 1e-8);
    }

    @Test
    public void testLongTypeAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgLongType(new long[] { (long) 10, (long) 15, (long) 13 }),
            1e-8);
    }

    @Test
    public void testFloatTypeAvg() {
        assertEquals((float) 12.666666984558105,
            instance.testAvgFloatType(new float[] { (float) 10, (float) 15, (float) 13 }),
            1e-15);
    }

    @Test
    public void testDoubleTypeAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgDoubleType(new double[] { (double) 10, (double) 15, (double) 13 }),
            1e-8);
    }

    @Test
    public void testByteAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgByte(new java.lang.Byte[] { java.lang.Byte.valueOf((byte) 10),
                    java.lang.Byte.valueOf((byte) 15),
                    java.lang.Byte.valueOf((byte) 13) }),
            1e-8);
    }

    @Test
    public void testShortAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgShort(new java.lang.Short[] { java.lang.Short.valueOf((short) 10),
                    java.lang.Short.valueOf((short) 15),
                    java.lang.Short.valueOf((short) 13) }),
            1e-8);
    }

    @Test
    public void testIntegerAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgInteger(new java.lang.Integer[] { java.lang.Integer.valueOf(10),
                    java.lang.Integer.valueOf(15),
                    java.lang.Integer.valueOf(13) }),
            1e-8);
    }

    @Test
    public void testLongAvg() {
        assertEquals(12.666666666666666d,
            instance.testAvgLong(new java.lang.Long[] { java.lang.Long.valueOf((long) 10),
                    java.lang.Long.valueOf((long) 15),
                    java.lang.Long.valueOf((long) 13) }),
            1e-8);
    }

    @Test
    public void testFloatAvg() {
        assertEquals(java.lang.Float.valueOf((float) 12.666667),
            instance.testAvgFloat(new java.lang.Float[] { java.lang.Float.valueOf((float) 10),
                    java.lang.Float.valueOf((float) 15),
                    java.lang.Float.valueOf((float) 13) }),
            1e-15);
    }

    @Test
    public void testDoubleAvg() {
        assertEquals(java.lang.Double.valueOf((double) 12.666666666666666),
            instance.testAvgDouble(new java.lang.Double[] { java.lang.Double.valueOf((double) 10),
                    java.lang.Double.valueOf((double) 15),
                    java.lang.Double.valueOf((double) 13) }),
            1e-15);
    }

    @Test
    public void testBigDecimalValueAvg() {
        assertEquals(new BigDecimalValue("12.666667"),
            instance.testAvgBigDecimal(new BigDecimalValue[] { new BigDecimalValue("10"),
                    new BigDecimalValue("15"),
                    new BigDecimalValue("13") }));

    }

    @Test
    public void testBigIntegerlValueAvg() {
        assertEquals(new BigDecimalValue("12.66666666666").doubleValue(),
            instance.testAvgBigInteger(new BigIntegerValue[] { new BigIntegerValue("10"),
                    new BigIntegerValue("15"),
                    new BigIntegerValue("13") })
                .doubleValue(),
            0.001);

    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallByteType() {
        byte[] array = { 10, 32, 35, 25 };
        assertEquals(10, instance.testByteTypeSmall(array, 1));
        assertEquals(25, instance.testByteTypeSmall(array, 2));
        assertEquals(32, instance.testByteTypeSmall(array, 3));
        assertEquals(35, instance.testByteTypeSmall(array, 4));

        instance.testByteTypeSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallShortType() {
        short[] array = { 10, 32, 35, 25 };
        assertEquals(10, instance.testShortTypeSmall(array, 1));
        assertEquals(25, instance.testShortTypeSmall(array, 2));
        assertEquals(32, instance.testShortTypeSmall(array, 3));
        assertEquals(35, instance.testShortTypeSmall(array, 4));

        instance.testShortTypeSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallIntegerType() {
        int[] array = { 10, 32, 35, 25 };
        assertEquals(10, instance.testIntegerTypeSmall(array, 1));
        assertEquals(25, instance.testIntegerTypeSmall(array, 2));
        assertEquals(32, instance.testIntegerTypeSmall(array, 3));
        assertEquals(35, instance.testIntegerTypeSmall(array, 4));

        instance.testIntegerTypeSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallLongType() {
        long[] array = { 10, 32, 35, 25 };
        assertEquals(10, instance.testLongTypeSmall(array, 1));
        assertEquals(25, instance.testLongTypeSmall(array, 2));
        assertEquals(32, instance.testLongTypeSmall(array, 3));
        assertEquals(35, instance.testLongTypeSmall(array, 4));

        instance.testLongTypeSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallFloatType() {
        float[] array = { (float) 10.1, (float) 32.2, (float) 35.4, (float) 25.5 };
        assertEquals(instance.testFloatTypeSmall(array, 1), (float) 10.1, 0.0);
        assertEquals(instance.testFloatTypeSmall(array, 2), (float) 25.5, 0.0);
        assertEquals(instance.testFloatTypeSmall(array, 3), (float) 32.2, 0.0);
        assertEquals(instance.testFloatTypeSmall(array, 4), (float) 35.4, 0.0);

        instance.testFloatTypeSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallDoubleType() {
        double[] array = { 10.1, 32.2, 35.3, 25.4 };
        assertEquals(10.1, instance.testDoubleTypeSmall(array, 1), 0.0);
        assertEquals(25.4, instance.testDoubleTypeSmall(array, 2), 0.0);
        assertEquals(32.2, instance.testDoubleTypeSmall(array, 3), 0.0);
        assertEquals(35.3, instance.testDoubleTypeSmall(array, 4), 0.0);

        instance.testDoubleTypeSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallBigDecimal() {
        BigDecimal[] array = { BigDecimal.valueOf(10),
                BigDecimal.valueOf(32),
                BigDecimal.valueOf(35),
                BigDecimal.valueOf(25) };
        assertEquals(instance.testBigDecimalSmall(array, 1), BigDecimal.valueOf(10));
        assertEquals(instance.testBigDecimalSmall(array, 2), BigDecimal.valueOf(25));
        assertEquals(instance.testBigDecimalSmall(array, 3), BigDecimal.valueOf(32));
        assertEquals(instance.testBigDecimalSmall(array, 4), BigDecimal.valueOf(35));

        instance.testBigDecimalSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallBigInteger() {
        BigInteger[] array = { BigInteger.valueOf(10),
                BigInteger.valueOf(32),
                BigInteger.valueOf(35),
                BigInteger.valueOf(25) };
        assertEquals(instance.testBigIntegerSmall(array, 1), BigInteger.valueOf(10));
        assertEquals(instance.testBigIntegerSmall(array, 2), BigInteger.valueOf(25));
        assertEquals(instance.testBigIntegerSmall(array, 3), BigInteger.valueOf(32));
        assertEquals(instance.testBigIntegerSmall(array, 4), BigInteger.valueOf(35));

        instance.testBigIntegerSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallByte() {
        java.lang.Byte[] array = { 10, 32, 35, 25 };
        assertSame(instance.testByteSmall(array, 1), Byte.valueOf((byte) 10));
        assertSame(instance.testByteSmall(array, 2), Byte.valueOf((byte) 25));
        assertSame(instance.testByteSmall(array, 3), Byte.valueOf((byte) 32));
        assertSame(instance.testByteSmall(array, 4), Byte.valueOf((byte) 35));

        instance.testByteSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallShort() {
        java.lang.Short[] array = { 10, 32, 35, 25 };
        assertSame(instance.testShortSmall(array, 1), Short.valueOf((short) 10));
        assertSame(instance.testShortSmall(array, 2), Short.valueOf((short) 25));
        assertSame(instance.testShortSmall(array, 3), Short.valueOf((short) 32));
        assertSame(instance.testShortSmall(array, 4), Short.valueOf((short) 35));

        instance.testShortSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallInteger() {
        java.lang.Integer[] array = { 10, 32, 35, 25 };
        assertSame(instance.testIntegerSmall(array, 1), Integer.valueOf(10));
        assertSame(instance.testIntegerSmall(array, 2), Integer.valueOf(25));
        assertSame(instance.testIntegerSmall(array, 3), Integer.valueOf(32));
        assertSame(instance.testIntegerSmall(array, 4), Integer.valueOf(35));

        instance.testIntegerSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallLong() {
        java.lang.Long[] array = { java.lang.Long.valueOf(10),
                java.lang.Long.valueOf(32),
                java.lang.Long.valueOf(35),
                java.lang.Long.valueOf(25) };
        assertTrue(instance.testLongSmall(array, 1) == java.lang.Long.valueOf(10));
        assertTrue(instance.testLongSmall(array, 2) == java.lang.Long.valueOf(25));
        assertTrue(instance.testLongSmall(array, 3) == java.lang.Long.valueOf(32));
        assertTrue(instance.testLongSmall(array, 4) == java.lang.Long.valueOf(35));

        instance.testLongSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallFloat() {
        java.lang.Float[] array = { java.lang.Float.valueOf((float) 10.4),
                java.lang.Float.valueOf((float) 32.1),
                java.lang.Float.valueOf((float) 35.3),
                java.lang.Float.valueOf((float) 25.7) };
        assertTrue(instance.testFloatSmall(array, 1).equals(java.lang.Float.valueOf((float) 10.4)));
        assertTrue(instance.testFloatSmall(array, 2).equals(java.lang.Float.valueOf((float) 25.7)));
        assertTrue(instance.testFloatSmall(array, 3).equals(java.lang.Float.valueOf((float) 32.1)));
        assertTrue(instance.testFloatSmall(array, 4).equals(java.lang.Float.valueOf((float) 35.3)));

        instance.testFloatSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testSmallDouble() {
        java.lang.Double[] array = { java.lang.Double.valueOf(10.4),
                java.lang.Double.valueOf(32.2),
                java.lang.Double.valueOf(35.6),
                java.lang.Double.valueOf(25.2) };
        assertTrue(instance.testDoubleSmall(array, 1).equals(java.lang.Double.valueOf(10.4)));
        assertTrue(instance.testDoubleSmall(array, 2).equals(java.lang.Double.valueOf(25.2)));
        assertTrue(instance.testDoubleSmall(array, 3).equals(java.lang.Double.valueOf(32.2)));
        assertTrue(instance.testDoubleSmall(array, 4).equals(java.lang.Double.valueOf(35.6)));

        instance.testDoubleSmall(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigByteType() {
        byte[] array = { 10, 32, 35, 25 };
        assertTrue(instance.testByteTypeBig(array, 4) == 10);
        assertTrue(instance.testByteTypeBig(array, 3) == 25);
        assertTrue(instance.testByteTypeBig(array, 2) == 32);
        assertTrue(instance.testByteTypeBig(array, 1) == 35);

        instance.testByteTypeBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigShortType() {
        short[] array = { 10, 32, 35, 25 };
        assertTrue(instance.testShortTypeBig(array, 4) == 10);
        assertTrue(instance.testShortTypeBig(array, 3) == 25);
        assertTrue(instance.testShortTypeBig(array, 2) == 32);
        assertTrue(instance.testShortTypeBig(array, 1) == 35);

        instance.testShortTypeBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigIntegerType() {
        int[] array = { 10, 32, 35, 25 };
        assertTrue(instance.testIntegerTypeBig(array, 4) == 10);
        assertTrue(instance.testIntegerTypeBig(array, 3) == 25);
        assertTrue(instance.testIntegerTypeBig(array, 2) == 32);
        assertTrue(instance.testIntegerTypeBig(array, 1) == 35);

        instance.testIntegerTypeBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigLongType() {
        long[] array = { 10, 32, 35, 25 };
        assertTrue(instance.testLongTypeBig(array, 4) == 10);
        assertTrue(instance.testLongTypeBig(array, 3) == 25);
        assertTrue(instance.testLongTypeBig(array, 2) == 32);
        assertTrue(instance.testLongTypeBig(array, 1) == 35);

        instance.testLongTypeBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigFloatType() {
        float[] array = { (float) 10.1, (float) 32.2, (float) 35.4, (float) 25.5 };
        assertTrue(instance.testFloatTypeBig(array, 4) == (float) 10.1);
        assertTrue(instance.testFloatTypeBig(array, 3) == (float) 25.5);
        assertTrue(instance.testFloatTypeBig(array, 2) == (float) 32.2);
        assertTrue(instance.testFloatTypeBig(array, 1) == (float) 35.4);

        instance.testFloatTypeBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigDoubleType() {
        double[] array = { 10.1, 32.2, 35.3, 25.4 };
        assertTrue(instance.testDoubleTypeBig(array, 4) == 10.1);
        assertTrue(instance.testDoubleTypeBig(array, 3) == 25.4);
        assertTrue(instance.testDoubleTypeBig(array, 2) == 32.2);
        assertTrue(instance.testDoubleTypeBig(array, 1) == 35.3);

        instance.testDoubleTypeBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigBigDecimal() {
        BigDecimal[] array = { BigDecimal.valueOf(10),
                BigDecimal.valueOf(32),
                BigDecimal.valueOf(35),
                BigDecimal.valueOf(25) };
        assertTrue(instance.testBigDecimalBig(array, 4).equals(BigDecimal.valueOf(10)));
        assertTrue(instance.testBigDecimalBig(array, 3).equals(BigDecimal.valueOf(25)));
        assertTrue(instance.testBigDecimalBig(array, 2).equals(BigDecimal.valueOf(32)));
        assertTrue(instance.testBigDecimalBig(array, 1).equals(BigDecimal.valueOf(35)));

        instance.testBigDecimalBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigBigInteger() {
        BigInteger[] array = { BigInteger.valueOf(10),
                BigInteger.valueOf(32),
                BigInteger.valueOf(35),
                BigInteger.valueOf(25) };
        assertTrue(instance.testBigIntegerBig(array, 4).equals(BigInteger.valueOf(10)));
        assertTrue(instance.testBigIntegerBig(array, 3).equals(BigInteger.valueOf(25)));
        assertTrue(instance.testBigIntegerBig(array, 2).equals(BigInteger.valueOf(32)));
        assertTrue(instance.testBigIntegerBig(array, 1).equals(BigInteger.valueOf(35)));

        instance.testBigIntegerBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigByte() {
        java.lang.Byte[] array = { 10, 32, 35, 25 };
        assertTrue(instance.testByteBig(array, 4) == java.lang.Byte.valueOf((byte) 10));
        assertTrue(instance.testByteBig(array, 3) == java.lang.Byte.valueOf((byte) 25));
        assertTrue(instance.testByteBig(array, 2) == java.lang.Byte.valueOf((byte) 32));
        assertTrue(instance.testByteBig(array, 1) == java.lang.Byte.valueOf((byte) 35));

        instance.testByteBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigShort() {
        java.lang.Short[] array = { 10, 32, 35, 25 };
        assertTrue(instance.testShortBig(array, 4) == java.lang.Short.valueOf((short) 10));
        assertTrue(instance.testShortBig(array, 3) == java.lang.Short.valueOf((short) 25));
        assertTrue(instance.testShortBig(array, 2) == java.lang.Short.valueOf((short) 32));
        assertTrue(instance.testShortBig(array, 1) == java.lang.Short.valueOf((short) 35));

        instance.testShortBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigInteger() {
        java.lang.Integer[] array = { 10, 32, 35, 25 };
        assertTrue(instance.testIntegerBig(array, 4) == java.lang.Integer.valueOf(10));
        assertTrue(instance.testIntegerBig(array, 3) == java.lang.Integer.valueOf(25));
        assertTrue(instance.testIntegerBig(array, 2) == java.lang.Integer.valueOf(32));
        assertTrue(instance.testIntegerBig(array, 1) == java.lang.Integer.valueOf(35));

        instance.testIntegerBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigLong() {
        java.lang.Long[] array = { java.lang.Long.valueOf(10),
                java.lang.Long.valueOf(32),
                java.lang.Long.valueOf(35),
                java.lang.Long.valueOf(25) };
        assertTrue(instance.testLongBig(array, 4) == java.lang.Long.valueOf(10));
        assertTrue(instance.testLongBig(array, 3) == java.lang.Long.valueOf(25));
        assertTrue(instance.testLongBig(array, 2) == java.lang.Long.valueOf(32));
        assertTrue(instance.testLongBig(array, 1) == java.lang.Long.valueOf(35));

        instance.testLongBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigFloat() {
        java.lang.Float[] array = { java.lang.Float.valueOf((float) 10.4),
                java.lang.Float.valueOf((float) 32.1),
                java.lang.Float.valueOf((float) 35.3),
                java.lang.Float.valueOf((float) 25.7) };
        assertTrue(instance.testFloatBig(array, 4).equals(java.lang.Float.valueOf((float) 10.4)));
        assertTrue(instance.testFloatBig(array, 3).equals(java.lang.Float.valueOf((float) 25.7)));
        assertTrue(instance.testFloatBig(array, 2).equals(java.lang.Float.valueOf((float) 32.1)));
        assertTrue(instance.testFloatBig(array, 1).equals(java.lang.Float.valueOf((float) 35.3)));

        instance.testFloatBig(array, 0);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testBigDouble() {
        java.lang.Double[] array = { java.lang.Double.valueOf(10.4),
                java.lang.Double.valueOf(32.2),
                java.lang.Double.valueOf(35.6),
                java.lang.Double.valueOf(25.2) };
        assertTrue(instance.testDoubleBig(array, 4).equals(java.lang.Double.valueOf(10.4)));
        assertTrue(instance.testDoubleBig(array, 3).equals(java.lang.Double.valueOf(25.2)));
        assertTrue(instance.testDoubleBig(array, 2).equals(java.lang.Double.valueOf(32.2)));
        assertTrue(instance.testDoubleBig(array, 1).equals(java.lang.Double.valueOf(35.6)));

        instance.testDoubleBig(array, 0);
    }

    @Test
    public void testByteValueQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientByteValue(new ByteValue((byte) 25), new ByteValue((byte) 12)));
    }

    @Test
    public void testShortValueQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientShortValue(new ShortValue((short) 25), new ShortValue((short) 12)));
    }

    @Test
    public void testIntegerValueQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientIntegerValue(new IntValue(25), new IntValue(12)));
    }

    @Test
    public void testLongValueQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientLongValue(new LongValue(25), new LongValue((long) 12)));
    }

    @Test
    public void testFloatValueQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientFloatValue(new FloatValue((float) 25.4), new FloatValue((float) 12.2)));
    }

    @Test
    public void testDoubleValueQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientDoubleValue(new DoubleValue(25.4), new DoubleValue(12.2)));
    }

    @Test
    public void testBigIntegerValueQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientBigIntegerValue(new BigIntegerValue(BigInteger.valueOf(25)),
                new BigIntegerValue(BigInteger.valueOf(12))));
    }

    @Test
    public void testBigDecimalValueQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientBigDecimalValue(new BigDecimalValue(BigDecimal.valueOf(25.4)),
                new BigDecimalValue(BigDecimal.valueOf(12.2))));
    }

    @Test
    public void testByteTypeQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientByteType((byte) 25, (byte) 12));
    }

    @Test
    public void testShortTypeQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientShortType((short) 25, (short) 12));
    }

    @Test
    public void testIntegerTypeQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientIntegerType(25, 12));
    }

    @Test
    public void testLongTypeQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientLongType((long) 25, (long) 12));
    }

    @Test
    public void testFloatTypeQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientFloatType((float) 25.5, (float) 12.2));
    }

    @Test
    public void testDoubleTypeQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientDoubleType(25.4, 12.2));
    }

    @Test
    public void testBigIntegerQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientBigInteger(BigInteger.valueOf(25), BigInteger.valueOf(12)));
    }

    @Test
    public void testBigDecimalQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientBigDecimal(BigDecimal.valueOf(25.4), BigDecimal.valueOf(12.2)));
    }

    @Test
    public void testByteQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientByte(Byte.valueOf((byte) 25), Byte.valueOf((byte) 12)));
    }

    @Test
    public void testShortQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientShort(Short.valueOf((short) 25), Short.valueOf((short) 12)));
    }

    @Test
    public void testIntegerQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientInteger(Integer.valueOf(25), Integer.valueOf(12)));
    }

    @Test
    public void testLongQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientLong(Long.valueOf((long) 25), Long.valueOf((long) 12)));
    }

    @Test
    public void testFloatQuaotient() {
        assertEquals(new LongValue(2),
            instance.testQuaotientFloat(Float.valueOf((float) 25.4), Float.valueOf((float) 12.2)));
    }

    @Test
    public void testDoubleQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotientDouble(Double.valueOf(25.4), Double.valueOf(12.2)));
    }

    @Test
    public void testByteValueMin() {
        assertEquals(new ByteValue((byte) 1),
            instance
                .testMinByteValue(new ByteValue[] { new ByteValue("10"), new ByteValue("15"), new ByteValue("1") }));
    }

    @Test
    public void testShortValueMin() {
        assertEquals(new ShortValue((short) 1),
            instance.testMinShortValue(
                new ShortValue[] { new ShortValue("10"), new ShortValue("15"), new ShortValue("1") }));
    }

    @Test
    public void testIntegerValueMin() {
        assertEquals(new IntValue(1),
            instance.testMinIntegerValue(new IntValue[] { new IntValue("10"), new IntValue("15"), new IntValue("1") }));
    }

    @Test
    public void testLongValueMin() {
        assertEquals(new LongValue((long) 1),
            instance
                .testMinLongValue(new LongValue[] { new LongValue("10"), new LongValue("15"), new LongValue("1") }));
    }

    @Test
    public void testFloatValueMin() {
        assertEquals(new FloatValue((float) 1.1),
            instance.testMinFloatValue(
                new FloatValue[] { new FloatValue("10.3"), new FloatValue("15.7"), new FloatValue("1.1") }));
    }

    @Test
    public void testDoubleValueMin() {
        assertEquals(new DoubleValue(1.1),
            instance.testMinDoubleValue(
                new DoubleValue[] { new DoubleValue("10.5"), new DoubleValue("15.7"), new DoubleValue("1.1") }));
    }

    @Test
    public void testBigIntegerValueMin() {
        assertEquals(new BigIntegerValue("1"),
            instance.testMinBigIntegerValue(new BigIntegerValue[] { new BigIntegerValue("10"),
                    new BigIntegerValue("15"),
                    new BigIntegerValue("1") }));
    }

    @Test
    public void testBigDecimalValueMin() {
        assertEquals(new BigDecimalValue("1.1"),
            instance.testMinBigDecimalValue(new BigDecimalValue[] { new BigDecimalValue("10.1"),
                    new BigDecimalValue("15.1"),
                    new BigDecimalValue("1.1") }));
    }

    @Test
    public void testByteValueMax() {
        assertEquals(new ByteValue((byte) 15),
            instance
                .testMaxByteValue(new ByteValue[] { new ByteValue("10"), new ByteValue("15"), new ByteValue("1") }));
    }

    @Test
    public void testShortValueMax() {
        assertEquals(new ShortValue((short) 15),
            instance.testMaxShortValue(
                new ShortValue[] { new ShortValue("10"), new ShortValue("15"), new ShortValue("1") }));
    }

    @Test
    public void testIntegerValueMax() {
        assertEquals(new IntValue(15),
            instance.testMaxIntegerValue(new IntValue[] { new IntValue("10"), new IntValue("15"), new IntValue("1") }));
    }

    @Test
    public void testLongValueMax() {
        assertEquals(new LongValue((long) 15),
            instance
                .testMaxLongValue(new LongValue[] { new LongValue("10"), new LongValue("15"), new LongValue("1") }));
    }

    @Test
    public void testFloatValueMax() {
        assertEquals(new FloatValue((float) 15.7),
            instance.testMaxFloatValue(
                new FloatValue[] { new FloatValue("10.3"), new FloatValue("15.7"), new FloatValue("1.1") }));
    }

    @Test
    public void testDoubleValueMax() {
        assertEquals(new DoubleValue(15.7),
            instance.testMaxDoubleValue(
                new DoubleValue[] { new DoubleValue("10.5"), new DoubleValue("15.7"), new DoubleValue("1.1") }));
    }

    @Test
    public void testBigIntegerValueMax() {
        assertEquals(new BigIntegerValue("15"),
            instance.testMaxBigIntegerValue(new BigIntegerValue[] { new BigIntegerValue("10"),
                    new BigIntegerValue("15"),
                    new BigIntegerValue("1") }));
    }

    @Test
    public void testBigDecimalValueMax() {
        assertEquals(new BigDecimalValue("15.1"),
            instance.testMaxBigDecimalValue(new BigDecimalValue[] { new BigDecimalValue("10.1"),
                    new BigDecimalValue("15.1"),
                    new BigDecimalValue("1.1") }));
    }

    @Test
    public void testByteValueMod() {
        assertEquals(new ByteValue((byte) 1),
            instance.testModByteValue(new ByteValue((byte) 10), new ByteValue((byte) 3)));
    }

    @Test
    public void testShortValueMod() {
        assertEquals(new ShortValue((short) 1),
            instance.testModShortValue(new ShortValue((short) 10), new ShortValue((short) 3)));
    }

    @Test
    public void testIntegerValueMod() {
        assertEquals(new IntValue(1), instance.testModIntegerValue(new IntValue(10), new IntValue(3)));
    }

    @Test
    public void testLongValueMod() {
        assertEquals(new LongValue((long) 1),
            instance.testModLongValue(new LongValue((long) 10), new LongValue((long) 3)));
    }

    @Test
    public void testFloatValueMod() {
        assertEquals(new FloatValue((float) 0.5),
            instance.testModFloatValue(new FloatValue((float) 10.1), new FloatValue((float) 3.2)));
    }

    @Test
    public void testBigDecimalValueMod() {
        assertEquals(new BigDecimalValue(BigDecimal.valueOf(0.5)),
            instance.testModBigDecimalValue(new BigDecimalValue(BigDecimal.valueOf(10.1)),
                new BigDecimalValue(BigDecimal.valueOf(3.2))));
    }

    @Test
    public void testBigIntegerValueMod() {
        assertEquals(new BigIntegerValue(BigInteger.valueOf(1)),
            instance.testModBigIntegerValue(new BigIntegerValue(BigInteger.valueOf(10)),
                new BigIntegerValue(BigInteger.valueOf(3))));
    }

    @Test
    public void testByteMod() {
        assertEquals(new Byte((byte) 1), instance.testModByte(new Byte((byte) 10), new Byte((byte) 3)));
    }

    @Test
    public void testShortMod() {
        assertEquals(new Short((short) 1), instance.testModShort(new Short((short) 10), new Short((short) 3)));
    }

    @Test
    public void testIntegerMod() {
        assertEquals(new Integer(1), instance.testModInteger(new Integer(10), new Integer(3)));
    }

    @Test
    public void testLongMod() {
        assertEquals(new Long((long) 1), instance.testModLong(new Long((long) 10), new Long((long) 3)));
    }

    @Test
    public void testFloatMod() {
        assertEquals(new Float((float) 0.5), instance.testModFloat(new Float((float) 10.1), new Float((float) 3.2)));
    }

    @Test
    public void testBigDecimalMod() {
        assertEquals(new BigDecimal(0.5).setScale(3, RoundingMode.HALF_UP),
            instance.testModBigDecimal(new BigDecimal(10.1), new BigDecimal(3.2)).setScale(3, RoundingMode.HALF_UP));
    }

    @Test
    public void testBigIntegerMod() {
        assertEquals(BigInteger.valueOf(1), instance.testModBigInteger(BigInteger.valueOf(10), BigInteger.valueOf(3)));
    }

    @Test
    public void testByteTypeMod() {
        assertEquals((byte) 1, instance.testModByteType((byte) 10, (byte) 3));
    }

    @Test
    public void testShortTypeMod() {
        assertEquals((short) 1, instance.testModShortType((short) 10, (short) 3));
    }

    @Test
    public void testIntegerTypeMod() {
        assertEquals(1, instance.testModIntegerType(10, 3));
    }

    @Test
    public void testLongModType() {
        assertEquals((long) 1, instance.testModLongType((long) 10, (long) 3));
    }

    @Test
    public void testFloatModType() {
        assertEquals((float) 0.5, instance.testModFloatType((float) 10.1, (float) 3.2), 1e-15);
    }

    @Test
    public void testByteSlice() {
        assertArrayEquals(
            new Byte[] { new Byte(
                (byte) 3), new Byte((byte) 4), new Byte((byte) 5), new Byte((byte) 6), new Byte((byte) 7) },
            instance
                .testSliceByte(
                    new Byte[] { new Byte((byte) 1),
                            new Byte((byte) 2),
                            new Byte((byte) 3),
                            new Byte((byte) 4),
                            new Byte((byte) 5),
                            new Byte((byte) 6),
                            new Byte((byte) 7) },
                    2));
    }

    @Test
    public void testByteSliceEndIndex() {
        assertArrayEquals(new Byte[] { new Byte((byte) 3), new Byte((byte) 4), new Byte((byte) 5) },
            instance
                .testSliceByte(
                    new Byte[] { new Byte((byte) 1),
                            new Byte((byte) 2),
                            new Byte((byte) 3),
                            new Byte((byte) 4),
                            new Byte((byte) 5),
                            new Byte((byte) 6),
                            new Byte((byte) 7) },
                    2,
                    5));
    }

    @Test
    public void testShortSlice() {
        assertArrayEquals(
            new Short[] { new Short(
                (short) 3), new Short((short) 4), new Short((short) 5), new Short((short) 6), new Short((short) 7) },
            instance
                .testSliceShort(
                    new Short[] { new Short((short) 1),
                            new Short((short) 2),
                            new Short((short) 3),
                            new Short((short) 4),
                            new Short((short) 5),
                            new Short((short) 6),
                            new Short((short) 7) },
                    2));
    }

    @Test
    public void testShortSliceEndIndex() {
        assertArrayEquals(new Short[] { new Short((short) 3), new Short((short) 4), new Short((short) 5) },
            instance
                .testSliceShort(
                    new Short[] { new Short((short) 1),
                            new Short((short) 2),
                            new Short((short) 3),
                            new Short((short) 4),
                            new Short((short) 5),
                            new Short((short) 6),
                            new Short((short) 7) },
                    2,
                    5));
    }

    @Test
    public void testIntegerSlice() {
        assertArrayEquals(
            new Integer[] { new Integer(3), new Integer(4), new Integer(5), new Integer(6), new Integer(7) },
            instance.testSliceInteger(new Integer[] { new Integer(
                1), new Integer(2), new Integer(3), new Integer(4), new Integer(5), new Integer(6), new Integer(7) },
                2));
    }

    @Test
    public void testIntegerSliceEndIndex() {
        assertArrayEquals(new Integer[] { new Integer(3), new Integer(4), new Integer(5) },
            instance.testSliceInteger(new Integer[] { new Integer(
                1), new Integer(2), new Integer(3), new Integer(4), new Integer(5), new Integer(6), new Integer(7) },
                2,
                5));
    }

    @Test
    public void testLongSlice() {
        assertArrayEquals(new Long[] { new Long(3), new Long(4), new Long(5), new Long(6), new Long(7) },
            instance
                .testSliceLong(
                    new Long[] { new Long(
                        1), new Long(2), new Long(3), new Long(4), new Long(5), new Long(6), new Long(7) },
                    2));
    }

    @Test
    public void testLongSliceEndIndex() {
        assertArrayEquals(new Long[] { new Long(3), new Long(4), new Long(5) },
            instance
                .testSliceLong(
                    new Long[] { new Long(
                        1), new Long(2), new Long(3), new Long(4), new Long(5), new Long(6), new Long(7) },
                    2,
                    5));
    }

    @Test
    public void testFloatSlice() {
        assertArrayEquals(new Float[] { new Float(3), new Float(4), new Float(5), new Float(6), new Float(7) },
            instance
                .testSliceFloat(
                    new Float[] { new Float(
                        1), new Float(2), new Float(3), new Float(4), new Float(5), new Float(6), new Float(7) },
                    2));
    }

    @Test
    public void testFloatSliceEndIndex() {
        assertArrayEquals(new Float[] { new Float(3.3), new Float(4.4), new Float(5.5) },
            instance.testSliceFloat(new Float[] { new Float(
                1.1), new Float(2.2), new Float(3.3), new Float(4.4), new Float(5.5), new Float(6.6), new Float(7.7) },
                2,
                5));
    }

    @Test
    public void testDoubleSlice() {
        assertArrayEquals(
            new Double[] { new Double(3.3), new Double(4.4), new Double(5.5), new Double(6.6), new Double(7.7) },
            instance
                .testSliceDouble(
                    new Double[] { new Double(1.1),
                            new Double(2.2),
                            new Double(3.3),
                            new Double(4.4),
                            new Double(5.5),
                            new Double(6.6),
                            new Double(7.7) },
                    2));
    }

    @Test
    public void testDoubleSliceEndIndex() {
        assertArrayEquals(new Double[] { new Double(3), new Double(4), new Double(5) },
            instance.testSliceDouble(
                new Double[] { new Double(
                    1), new Double(2), new Double(3), new Double(4), new Double(5), new Double(6), new Double(7) },
                2,
                5));
    }

    @Test
    public void testBigIntegerSlice() {
        assertArrayEquals(
            new BigInteger[] { BigInteger.valueOf(
                3), BigInteger.valueOf(4), BigInteger.valueOf(5), BigInteger.valueOf(6), BigInteger.valueOf(7) },
            instance
                .testSliceBigInteger(
                    new BigInteger[] { BigInteger.valueOf(1),
                            BigInteger.valueOf(2),
                            BigInteger.valueOf(3),
                            BigInteger.valueOf(4),
                            BigInteger.valueOf(5),
                            BigInteger.valueOf(6),
                            BigInteger.valueOf(7) },
                    2));
    }

    @Test
    public void testBigIntegerSliceEndIndex() {
        assertArrayEquals(new BigInteger[] { BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(5) },
            instance
                .testSliceBigInteger(
                    new BigInteger[] { BigInteger.valueOf(1),
                            BigInteger.valueOf(2),
                            BigInteger.valueOf(3),
                            BigInteger.valueOf(4),
                            BigInteger.valueOf(5),
                            BigInteger.valueOf(6),
                            BigInteger.valueOf(7) },
                    2,
                    5));
    }

    @Test
    public void testBigDecimalSlice() {
        assertArrayEquals(
            new BigDecimal[] { BigDecimal.valueOf(3.3),
                    BigDecimal.valueOf(4.4),
                    BigDecimal.valueOf(5.5),
                    BigDecimal.valueOf(6.6),
                    BigDecimal.valueOf(7.7) },
            instance
                .testSliceBigDecimal(
                    new BigDecimal[] { BigDecimal.valueOf(1.1),
                            BigDecimal.valueOf(2.2),
                            BigDecimal.valueOf(3.3),
                            BigDecimal.valueOf(4.4),
                            BigDecimal.valueOf(5.5),
                            BigDecimal.valueOf(6.6),
                            BigDecimal.valueOf(7.7) },
                    2));
    }

    @Test
    public void testBigDecimalSliceEndIndex() {
        assertArrayEquals(
            new BigDecimal[] { BigDecimal.valueOf(3.3), BigDecimal.valueOf(4.4), BigDecimal.valueOf(5.5) },
            instance
                .testSliceBigDecimal(
                    new BigDecimal[] { BigDecimal.valueOf(1.1),
                            BigDecimal.valueOf(2.2),
                            BigDecimal.valueOf(3.3),
                            BigDecimal.valueOf(4.4),
                            BigDecimal.valueOf(5.5),
                            BigDecimal.valueOf(6.6),
                            BigDecimal.valueOf(7.7) },
                    2,
                    5));
    }

    @Test
    public void testByteTypeSlice() {
        assertArrayEquals(new byte[] { (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7 },
            instance.testSliceByteType(
                new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7 },
                2));
    }

    @Test
    public void testByteTypeSliceEndIndex() {
        assertArrayEquals(new byte[] { (byte) 3, (byte) 4, (byte) 5 },
            instance.testSliceByteType(
                new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7 },
                2,
                5));
    }

    @Test
    public void testShortTypeSlice() {
        assertArrayEquals(new short[] { (short) 3, (short) 4, (short) 5, (short) 6, (short) 7 },
            instance.testSliceShortType(
                new short[] { (short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7 },
                2));
    }

    @Test
    public void testShortTypeSliceEndIndex() {
        assertArrayEquals(new short[] { (short) 3, (short) 4, (short) 5 },
            instance.testSliceShortType(
                new short[] { (short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7 },
                2,
                5));
    }

    @Test
    public void testIntegerTypeSlice() {
        assertArrayEquals(new int[] { 3, 4, 5, 6, 7 },
            instance.testSliceIntegerType(new int[] { 1, 2, 3, 4, 5, 6, 7 }, 2));
    }

    @Test
    public void testIntegerTypeSliceEndIndex() {
        assertArrayEquals(new int[] { 3, 4, 5 },
            instance.testSliceIntegerType(new int[] { 1, 2, 3, 4, 5, 6, 7 }, 2, 5));
    }

    @Test
    public void testLongTypeSlice() {
        assertArrayEquals(new long[] { (long) 3, (long) 4, (long) 5, (long) 6, (long) 7 },
            instance.testSliceLongType(
                new long[] { (long) 1, (long) 2, (long) 3, (long) 4, (long) 5, (long) 6, (long) 7 },
                2));
    }

    @Test
    public void testLongTypeSliceEndIndex() {
        assertArrayEquals(new long[] { (long) 3, (long) 4, (long) 5 },
            instance.testSliceLongType(
                new long[] { (long) 1, (long) 2, (long) 3, (long) 4, (long) 5, (long) 6, (long) 7 },
                2,
                5));
    }

    @Test
    public void testFloatTypeSlice() {
        assertArrayEquals(new float[] { (float) 3.3, (float) 4.4, (float) 5.5, (float) 6.6, (float) 7.7 },
            instance
                .testSliceFloatType(
                    new float[] { (float) 1.1,
                            (float) 2.2,
                            (float) 3.3,
                            (float) 4.4,
                            (float) 5.5,
                            (float) 6.6,
                            (float) 7.7 },
                    2),
            0.0001f);
    }

    @Test
    public void testFloatTypeSliceEndIndex() {
        assertArrayEquals(new float[] { (float) 3.3, (float) 4.4, (float) 5.5 },
            instance
                .testSliceFloatType(
                    new float[] { (float) 1.1,
                            (float) 2.2,
                            (float) 3.3,
                            (float) 4.4,
                            (float) 5.5,
                            (float) 6.6,
                            (float) 7.7 },
                    2,
                    5),
            0.0001f);
    }

    @Test
    public void testDoubleTypeSlice() {
        assertArrayEquals(new double[] { (double) 3.3, (double) 4.4, (double) 5.5, (double) 6.6, (double) 7.7 },
            instance
                .testSliceDoubleType(
                    new double[] { (double) 1.1,
                            (double) 2.2,
                            (double) 3.3,
                            (double) 4.4,
                            (double) 5.5,
                            (double) 6.6,
                            (double) 7.7 },
                    2),
            0.0001f);
    }

    @Test
    public void testDoubleTypeSliceEndIndex() {
        assertArrayEquals(new double[] { (double) 3.3, (double) 4.4, (double) 5.5 },
            instance
                .testSliceDoubleType(
                    new double[] { (double) 1.1,
                            (double) 2.2,
                            (double) 3.3,
                            (double) 4.4,
                            (double) 5.5,
                            (double) 6.6,
                            (double) 7.7 },
                    2,
                    5),
            0.0001f);
    }

    @Test
    public void testByteTypeSort() {
        byte[] inputArray = { 2, 1, 0 };
        byte[] nullArray = null;
        byte[] expectedArray = { 0, 1, 2 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortByteType(inputArray));
    }

    @Test
    public void testShortTypeSort() {
        short[] inputArray = { 2, 1, 0 };
        short[] nullArray = null;
        short[] expectedArray = { 0, 1, 2 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortShortType(inputArray));
    }

    @Test
    public void testIntegerTypeSort() {
        int[] inputArray = { 2, 1, 0 };
        int[] nullArray = null;
        int[] expectedArray = { 0, 1, 2 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortIntegerType(inputArray));
    }

    @Test
    public void testLongTypeSort() {
        long[] inputArray = { 2, 1, 0 };
        long[] nullArray = null;
        long[] expectedArray = { 0, 1, 2 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortLongType(inputArray));
    }

    @Test
    public void testFloatTypeSort() {
        float[] inputArray = { 2.1f, 1.1f, -0.4f };
        float[] nullArray = null;
        float[] expectedArray = { -0.4f, 1.1f, 2.1f };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortFloatType(inputArray), 0.00001f);
    }

    @Test
    public void testDoubleTypeSort() {
        double[] inputArray = { 2.1, 1.1, -0.4 };
        double[] nullArray = null;
        double[] expectedArray = { -0.4, 1.1, 2.1 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortDoubleType(inputArray), 0.00001f);
    }

    @Test
    public void testByteSort() {
        Byte[] inputArray = { 2, 1, 0 };
        Byte[] nullArray = null;
        Byte[] expectedArray = { 0, 1, 2 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortByte(inputArray));
    }

    @Test
    public void testShortSort() {
        Short[] inputArray = { 2, 1, 0 };
        Short[] nullArray = null;
        Short[] expectedArray = { 0, 1, 2 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortShort(inputArray));
    }

    @Test
    public void testIntegerSort() {
        Integer[] inputArray = { 2, 1, 0 };
        Integer[] nullArray = null;
        Integer[] expectedArray = { 0, 1, 2 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortInteger(inputArray));
    }

    @Test
    public void testLongSort() {
        Long[] inputArray = { 2l, 1l, 0l };
        Long[] nullArray = null;
        Long[] expectedArray = { 0l, 1l, 2l };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortLong(inputArray));
    }

    @Test
    public void testFloatSort() {
        Float[] inputArray = { 2.1f, 1.1f, -0.4f };
        Float[] nullArray = null;
        Float[] expectedArray = { -0.4f, 1.1f, 2.1f };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortFloat(inputArray));
    }

    @Test
    public void testDoubleSort() {
        Double[] inputArray = { 2.1, 1.1, -0.4 };
        Double[] nullArray = null;
        Double[] expectedArray = { -0.4, 1.1, 2.1 };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortDouble(inputArray));
    }

    @Test
    public void testByteValueSort() {
        ByteValue[] inputArray = { new ByteValue("2"), new ByteValue("1"), new ByteValue("0") };
        ByteValue[] nullArray = null;
        ByteValue[] expectedArray = { new ByteValue("0"), new ByteValue("1"), new ByteValue("2") };

        assertNull(ByteValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortByteValue(inputArray));
    }

    @Test
    public void testShortValueSort() {
        ShortValue[] inputArray = { new ShortValue((short) 2), new ShortValue("1"), new ShortValue("0") };
        ShortValue[] nullArray = null;
        ShortValue[] expectedArray = { new ShortValue("0"), new ShortValue("1"), new ShortValue("2") };

        assertNull(ShortValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortShortValue(inputArray));
    }

    @Test
    public void testIntegerValueSort() {
        IntValue[] inputArray = { new IntValue(2), new IntValue(1), new IntValue(0) };
        IntValue[] nullArray = null;
        IntValue[] expectedArray = { new IntValue(0), new IntValue(1), new IntValue(2) };

        assertNull(IntValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortIntegerValue(inputArray));
    }

    @Test
    public void testLongValueSort() {
        LongValue[] inputArray = { new LongValue(2l), new LongValue(1l), new LongValue(0l) };
        LongValue[] nullArray = null;
        LongValue[] expectedArray = { new LongValue(0l), new LongValue(1l), new LongValue(2l) };

        assertNull(LongValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortLongValue(inputArray));
    }

    @Test
    public void testFloatValueSort() {
        FloatValue[] inputArray = { new FloatValue(2.1f), new FloatValue(1.1f), new FloatValue(-0.4f) };
        FloatValue[] nullArray = null;
        FloatValue[] expectedArray = { new FloatValue(-0.4f), new FloatValue(1.1f), new FloatValue(2.1f) };

        assertNull(FloatValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortFloatValue(inputArray));
    }

    @Test
    public void testDoubleValueSort() {
        DoubleValue[] inputArray = { new DoubleValue(2.1), new DoubleValue(1.1), new DoubleValue(-0.4) };
        DoubleValue[] nullArray = null;
        DoubleValue[] expectedArray = { new DoubleValue(-0.4), new DoubleValue(1.1), new DoubleValue(2.1) };

        assertNull(DoubleValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortDoubleValue(inputArray));
    }

    @Test
    public void testBigIntegerSort() {
        BigInteger[] inputArray = { BigInteger.valueOf(2), BigInteger.valueOf(1), BigInteger.valueOf(-0) };
        BigInteger[] nullArray = null;
        BigInteger[] expectedArray = { BigInteger.valueOf(-0), BigInteger.valueOf(1), BigInteger.valueOf(2) };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortBigInteger(inputArray));
    }

    @Test
    public void testBigDecimalSort() {
        BigDecimal[] inputArray = { BigDecimal.valueOf(2.3), BigDecimal.valueOf(1.9), BigDecimal.valueOf(-0.1) };
        BigDecimal[] nullArray = null;
        BigDecimal[] expectedArray = { BigDecimal.valueOf(-0.1), BigDecimal.valueOf(1.9), BigDecimal.valueOf(2.3) };

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortBigDecimal(inputArray));
    }

    @Test
    public void testBigIntegerValueSort() {
        BigIntegerValue[] inputArray = { new BigIntegerValue("2"),
                new BigIntegerValue("1"),
                new BigIntegerValue("-0") };
        BigIntegerValue[] nullArray = null;
        BigIntegerValue[] expectedArray = { new BigIntegerValue("-0"),
                new BigIntegerValue("1"),
                new BigIntegerValue("2") };

        assertNull(BigIntegerValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortBigIntegerValue(inputArray));
    }

    @Test
    public void testBigDecimalValueSort() {
        BigDecimalValue[] inputArray = { new BigDecimalValue("2.3"),
                new BigDecimalValue("1.9"),
                new BigDecimalValue("-0.1") };
        BigDecimalValue[] nullArray = null;
        BigDecimalValue[] expectedArray = { new BigDecimalValue("-0.1"),
                new BigDecimalValue("1.9"),
                new BigDecimalValue("2.3") };

        assertNull(BigDecimalValue.sort(nullArray));
        assertArrayEquals(expectedArray, instance.testSortBigDecimalValue(inputArray));
    }

    @Test
    public void testStringSort() {

        String[] nullArray = null;
        String[] strValueArray = { null, "asd", "ac", null, null };
        String[] expecteds = { "ac", "asd", null, null, null };
        String[] actuals = instance.testSortString(strValueArray);

        assertNull(RulesUtils.sort(nullArray));
        assertArrayEquals(expecteds, actuals);

    }

    @Test
    public void testStringValueSort() {
        StringValue[] strValueArray = { null, new StringValue("asd"), new StringValue("ac"), null, null };
        StringValue[] expecteds = { new StringValue("ac"), new StringValue("asd"), null, null, null };
        StringValue[] actuals = instance.testSortStringValue(strValueArray);

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
        Date[] actuals = instance.testSortDate(nullDateArrayValue);
        Date[] expecteds = { c.getTime(), c.getTime(), null };

        assertNull(RulesUtils.sort(nullDateArray));
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testObjectValueSort() {
        ObjectValue[] strValueArray = { null, new ObjectValue("asd"), new ObjectValue("ac"), null, null };
        ObjectValue[] expecteds = { new ObjectValue("ac"), new ObjectValue("asd"), null, null, null };
        ObjectValue[] actuals = instance.testSortObjectValue(strValueArray);

        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testObjectInObjectArrContains() {
        Object searchFor = new ObjectValue("5");
        Object searchForFailed = new ObjectValue("666");
        Object[] searchIn = { new ObjectValue(
            "1"), new ObjectValue("4"), new ObjectValue("5"), new ObjectValue("7"), new ObjectValue("10") };

        assertFalse(instance.testContainsObjectInObjectArr(null, searchFor));
        assertFalse(instance.testContainsObjectInObjectArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsObjectInObjectArr(searchIn, searchFor));
    }

    @Test
    public void testContainsIntegerTypeInIntegerArr() {
        int searchFor = 5;
        int searchForFailed = 666;
        int[] searchIn = { 1, 4, 5, 7, 10 };

        assertFalse(instance.testContainsIntegerTypeInIntegerArr(null, searchFor));
        assertFalse(instance.testContainsIntegerTypeInIntegerArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsIntegerTypeInIntegerArr(searchIn, searchFor));
    }

    @Test
    public void testIntegerInIntegerArrContains() {
        Integer searchFor = 5;
        Integer searchForFailed = 666;
        Integer[] searchIn = { 1, 4, 5, null, 7, 10 };

        assertFalse(instance.testContainsIntegerInIntegerArr(null, searchFor));
        assertFalse(instance.testContainsIntegerInIntegerArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsIntegerInIntegerArr(searchIn, searchFor));
        assertTrue(instance.testContainsIntegerInIntegerArr(searchIn, null));
    }

    @Test
    public void testContainsLongTypeInLongArr() {
        long searchFor = 5L;
        long searchForFailed = 666L;
        long[] searchIn = { 1L, 4L, 5L, 7L, 10L };

        assertFalse(instance.testContainsLongTypeInLongArr(null, searchFor));
        assertFalse(instance.testContainsLongTypeInLongArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsLongTypeInLongArr(searchIn, searchFor));
    }

    @Test
    public void testLongInLongArrContains() {
        Long searchFor = 5L;
        Long searchForFailed = 666L;
        Long[] searchIn = { 1L, 4L, 5L, 7L, 10L };

        assertFalse(instance.testContainsLongInLongArr(null, searchFor));
        assertFalse(instance.testContainsLongInLongArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsLongInLongArr(searchIn, searchFor));
    }

    @Test
    public void testContainsByteTypeInByteArr() {
        byte searchFor = 5;
        byte searchForFailed = 127;
        byte[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsByteTypeInByteArr(null, searchFor));
        assertFalse(instance.testContainsByteTypeInByteArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsByteTypeInByteArr(searchIn, searchFor));
    }

    @Test
    public void testByteInByteArrContains() {
        Byte searchFor = 5;
        Byte searchForFailed = 127;
        Byte[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsByteInByteArr(null, searchFor));
        assertFalse(instance.testContainsByteInByteArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsByteInByteArr(searchIn, searchFor));
    }

    @Test
    public void testContainsShortTypeInShortArr() {
        short searchFor = 5;
        short searchForFailed = 32767;
        short[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsShortTypeInShortArr(null, searchFor));
        assertFalse(instance.testContainsShortTypeInShortArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsShortTypeInShortArr(searchIn, searchFor));
    }

    @Test
    public void testShortInShortArrContains() {
        Short searchFor = 5;
        Short searchForFailed = 32767;
        Short[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsShortInShortArr(null, searchFor));
        assertFalse(instance.testContainsShortInShortArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsShortInShortArr(searchIn, searchFor));
    }

    @Test
    public void testContainsCharTypeInCharArr() {
        char searchFor = 'Z';
        char searchForFailed = 'X';
        char[] searchIn = { 'a', 'b', 'c', 'Z', 'P' };

        assertFalse(instance.testContainsCharTypeInCharArr(null, searchFor));
        assertFalse(instance.testContainsCharTypeInCharArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsCharTypeInCharArr(searchIn, searchFor));
    }

    @Test
    public void testCharInCharArrContains() {
        Character searchFor = 'Z';
        Character searchForFailed = 'X';
        Character[] searchIn = { 'a', 'b', 'c', 'Z', 'P' };

        assertFalse(instance.testContainsCharInCharArr(null, searchFor));
        assertFalse(instance.testContainsCharInCharArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsCharInCharArr(searchIn, searchFor));
    }

    @Test
    public void testContainsFloatTypeInFloatArr() {
        float searchFor = 5.7f;
        float searchForFailed = 32767.321f;
        float[] searchIn = { 1.01f, 4.0f, 5.7f, -7.7f, 10.3f };

        assertFalse(instance.testContainsFloatTypeInFloatArr(null, searchFor));
        assertFalse(instance.testContainsFloatTypeInFloatArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsFloatTypeInFloatArr(searchIn, searchFor));
    }

    @Test
    public void testFloatInFloatArrContains() {
        Float searchFor = 5.7f;
        Float searchForFailed = 32767.321f;
        Float[] searchIn = { 1.01f, 4.0f, 5.7f, -7.7f, 10.3f };

        assertFalse(instance.testContainsFloatInFloatArr(null, searchFor));
        assertFalse(instance.testContainsFloatInFloatArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsFloatInFloatArr(searchIn, searchFor));
    }

    @Test
    public void testContainsDoubleTypeInDoubleArr() {
        double searchFor = 5.7;
        double searchForFailed = 32767.321;
        double[] searchIn = { 1.01, 4.0, 5.7, -7.7, 10.3 };

        assertFalse(instance.testContainsDoubleTypeInDoubleArr(null, searchFor));
        assertFalse(instance.testContainsDoubleTypeInDoubleArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsDoubleTypeInDoubleArr(searchIn, searchFor));
    }

    @Test
    public void testDoubleInDoubleArrContains() {
        Double searchFor = 5.7;
        Double searchForFailed = 32767.321;
        Double[] searchIn = { 1.01, 4.0, 5.7, -7.7, 10.3 };

        assertFalse(instance.testContainsDoubleInDoubleArr(null, searchFor));
        assertFalse(instance.testContainsDoubleInDoubleArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsDoubleInDoubleArr(searchIn, searchFor));
    }

    @Test
    public void testContainsBoolTypeInBoolArr() {
        boolean searchFor = true;
        boolean searchForFailed = false;
        boolean[] searchIn = { true, true, true, true, true };

        assertFalse(instance.testContainsBoolTypeInBoolArr(null, searchFor));
        assertFalse(instance.testContainsBoolTypeInBoolArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsBoolTypeInBoolArr(searchIn, searchFor));
    }

    @Test
    public void testBoolInBoolArrContains() {
        Boolean searchFor = true;
        Boolean searchForFailed = false;
        Boolean[] searchIn = { true, true, true, true, true };

        assertFalse(instance.testContainsBoolInBoolArr(null, searchFor));
        assertFalse(instance.testContainsBoolInBoolArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsBoolInBoolArr(searchIn, searchFor));
    }

    @Test
    public void testObjectArrInObjectArrContains() {
        Object[] searchFor = { new ObjectValue("5"), new ObjectValue("1") };
        Object[] searchForFailed = { new ObjectValue("666") };
        Object[] searchIn = { new ObjectValue(
            "1"), new ObjectValue("4"), new ObjectValue("5"), new ObjectValue("7"), new ObjectValue("10") };

        assertFalse(instance.testContainsObjectArrInObjectArr(searchIn, null));
        assertFalse(instance.testContainsObjectArrInObjectArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsObjectArrInObjectArr(searchIn, searchFor));
    }

    @Test
    public void testContainsIntegerTypeArrInIntegerArr() {
        int[] searchFor = { 5, 1 };
        int[] searchForFailed = { 666 };
        int[] searchIn = { 1, 4, 5, 7, 10 };

        assertFalse(instance.testContainsIntegerTypeArrInIntegerArr(searchIn, null));
        assertFalse(instance.testContainsIntegerTypeArrInIntegerArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsIntegerTypeArrInIntegerArr(searchIn, searchFor));
    }

    @Test
    public void testIntegerArrInIntegerArrContains() {
        Integer[] searchFor = { 5, 1 };
        Integer[] searchForFailed = { 666 };
        Integer[] searchIn = { 1, 4, 5, 7, 10 };

        assertFalse(instance.testContainsIntegerArrInIntegerArr(searchIn, null));
        assertFalse(instance.testContainsIntegerArrInIntegerArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsIntegerArrInIntegerArr(searchIn, searchFor));
    }

    @Test
    public void testContainsLongTypeArrInLongArr() {
        long[] searchFor = { 5l, 7l };
        long[] searchForFailed = { 666l };
        long[] searchIn = { 1l, 4l, 5l, 7l, 10l };

        assertFalse(instance.testContainsLongTypeArrInLongArr(searchIn, null));
        assertFalse(instance.testContainsLongTypeArrInLongArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsLongTypeArrInLongArr(searchIn, searchFor));
    }

    @Test
    public void testLongArrInLongArrContains() {
        Long[] searchFor = { 5l, 7l };
        Long[] searchForFailed = { 666l };
        Long[] searchIn = { 1l, 4l, 5l, 7l, 10l };

        assertFalse(instance.testContainsLongArrInLongArr(searchIn, null));
        assertFalse(instance.testContainsLongArrInLongArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsLongArrInLongArr(searchIn, searchFor));
    }

    @Test
    public void testContainsByteTypeArrInByteArr() {
        byte[] searchFor = { 5, -7 };
        byte[] searchForFailed = { 127 };
        byte[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsByteTypeArrInByteArr(searchIn, null));
        assertFalse(instance.testContainsByteTypeArrInByteArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsByteTypeArrInByteArr(searchIn, searchFor));
    }

    @Test
    public void testByteArrInByteArrContains() {
        Byte[] searchFor = { 5, -7 };
        Byte[] searchForFailed = { 127 };
        Byte[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsByteArrInByteArr(searchIn, null));
        assertFalse(instance.testContainsByteArrInByteArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsByteArrInByteArr(searchIn, searchFor));
    }

    @Test
    public void testContainsShortTypeArrInShortArr() {
        short[] searchFor = { 5, -7 };
        short[] searchForFailed = { 32767 };
        short[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsShortTypeArrInShortArr(searchIn, null));
        assertFalse(instance.testContainsShortTypeArrInShortArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsShortTypeArrInShortArr(searchIn, searchFor));
    }

    @Test
    public void testShortArrInShortArrContains() {
        Short[] searchFor = { 5, -7 };
        Short[] searchForFailed = { 32767 };
        Short[] searchIn = { 1, 4, 5, -7, 10 };

        assertFalse(instance.testContainsShortArrInShortArr(searchIn, null));
        assertFalse(instance.testContainsShortArrInShortArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsShortArrInShortArr(searchIn, searchFor));
    }

    @Test
    public void testContainsFloatTypeArrInFloatArr() {
        float[] searchFor = { 5.7f, -7.7f };
        float[] searchForFailed = { 32767.321f };
        float[] searchIn = { 1.01f, 4.0f, 5.7f, -7.7f, 10.3f };

        assertFalse(instance.testContainsFloatTypeArrInFloatArr(searchIn, null));
        assertFalse(instance.testContainsFloatTypeArrInFloatArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsFloatTypeArrInFloatArr(searchIn, searchFor));
    }

    @Test
    public void testFloatArrInFloatArrContains() {
        Float[] searchFor = { 5.7f, -7.7f };
        Float[] searchForFailed = { 32767.321f };
        Float[] searchIn = { 1.01f, 4.0f, 5.7f, -7.7f, 10.3f };

        assertFalse(instance.testContainsFloatArrInFloatArr(searchIn, null));
        assertFalse(instance.testContainsFloatArrInFloatArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsFloatArrInFloatArr(searchIn, searchFor));
    }

    @Test
    public void testContainsDoubleTypeArrInDoubleArr() {
        double[] searchFor = { 5.7, -7.7 };
        double[] searchForFailed = { 32767.321 };
        double[] searchIn = { 1.01, 4.0, 5.7, -7.7, 10.3 };

        assertFalse(instance.testContainsDoubleTypeArrInDoubleArr(searchIn, null));
        assertFalse(instance.testContainsDoubleTypeArrInDoubleArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsDoubleTypeArrInDoubleArr(searchIn, searchFor));
    }

    @Test
    public void testDoubleArrInDoubleArrContains() {
        Double[] searchFor = { 5.7, -7.7 };
        Double[] searchForFailed = { 32767.321 };
        Double[] searchIn = { 1.01, 4.0, 5.7, -7.7, 10.3 };

        assertFalse(instance.testContainsDoubleArrInDoubleArr(searchIn, null));
        assertFalse(instance.testContainsDoubleArrInDoubleArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsDoubleArrInDoubleArr(searchIn, searchFor));
    }

    @Test
    public void testContainsBoolTypeArrInBoolArr() {
        boolean[] searchFor = { true, true };
        boolean[] searchForFailed = { false };
        boolean[] searchIn = { true, true, true, true, true };

        assertFalse(instance.testContainsBoolTypeArrInBoolArr(searchIn, null));
        assertFalse(instance.testContainsBoolTypeArrInBoolArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsBoolTypeArrInBoolArr(searchIn, searchFor));
    }

    @Test
    public void testBoolArrInBoolArrContains() {
        Boolean[] searchFor = { true, true };
        Boolean[] searchForFailed = { false };
        Boolean[] searchIn = { true, true, true, true, true };

        assertFalse(instance.testContainsBoolArrInBoolArr(searchIn, null));
        assertFalse(instance.testContainsBoolArrInBoolArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsBoolArrInBoolArr(searchIn, searchFor));
    }

    @Test
    public void testContainsCharTypeArrInCharArr() {
        char[] searchFor = { 'Z', 'P' };
        char[] searchForFailed = { 'X' };
        char[] searchIn = { 'a', 'b', 'c', 'Z', 'P' };

        assertFalse(instance.testContainsCharTypeArrInCharArr(searchIn, null));
        assertFalse(instance.testContainsCharTypeArrInCharArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsCharTypeArrInCharArr(searchIn, searchFor));
    }

    @Test
    public void testCharArrInCharArrContains() {
        Character[] searchFor = { 'Z', 'P' };
        Character[] searchForFailed = { 'X' };
        Character[] searchIn = { 'a', 'b', 'c', 'Z', 'P' };

        assertFalse(instance.testContainsCharArrInCharArr(searchIn, null));
        assertFalse(instance.testContainsCharArrInCharArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsCharArrInCharArr(searchIn, searchFor));
    }

    @Test
    public void testContainsStringArrInStringArr() {
        String[] searchFor = { "foo", "bar" };
        String[] searchForFailed = { "X" };
        String[] searchIn = { "foo", "bar", "c", "Z", "P" };

        assertFalse(instance.testContainsStringArrInStringArr(searchIn, null));
        assertFalse(instance.testContainsStringArrInStringArr(searchIn, searchForFailed));
        assertTrue(instance.testContainsStringArrInStringArr(searchIn, searchFor));
    }

    @Test
    public void testObjectIndexOf() {
        assertEquals(-1, instance.testIndexOfObject(null, (Object) 3));
        assertEquals(-1, instance.testIndexOfObject(new Object[] { 1, 2, 3, 4, 5 }, (Object) 9));
        assertEquals(-1, instance.testIndexOfObject(new Object[] { 1, 2, 3, 4, 5 }, null));
        assertEquals(2, instance.testIndexOfObject(new Object[] { 1, 2, 3, 4, 5 }, (Object) 3));
    }

    @Test
    public void testIndexOfIntegerType() {
        assertEquals(-1, instance.testIndexOfIntegerType(null, 3));
        assertEquals(-1, instance.testIndexOfIntegerType(new int[] { 1, 2, 3, 4, 5 }, 9));
        assertEquals(2, instance.testIndexOfIntegerType(new int[] { 1, 2, 3, 4, 5 }, 3));
    }

    @Test
    public void testIntegerIndexOf() {
        assertEquals(-1, instance.testIndexOfInteger(null, 3));
        assertEquals(-1, instance.testIndexOfInteger(new Integer[] { 1, 2, 3, 4, 5 }, 9));
        assertEquals(2, instance.testIndexOfInteger(new Integer[] { 1, 2, 3, 4, 5 }, 3));
    }

    @Test
    public void testIndexOfLongType() {
        assertEquals(-1, instance.testIndexOfLongType(null, 3l));
        assertEquals(-1, instance.testIndexOfLongType(new long[] { 1, 2, 3, 4, 5 }, 9l));
        assertEquals(2, instance.testIndexOfLongType(new long[] { 1, 2, 3, 4, 5 }, 3l));
    }

    @Test
    public void testLongIndexOf() {
        assertEquals(-1, instance.testIndexOfLong(null, 3l));
        assertEquals(-1, instance.testIndexOfLong(new Long[] { 1L, 2L, 3L, 4L, 5L }, 9l));
        assertEquals(2, instance.testIndexOfLong(new Long[] { 1L, 2L, 3L, 4L, 5L }, 3l));
    }

    @Test
    public void testIndexOfByteType() {
        assertEquals(-1, instance.testIndexOfByteType(null, (byte) 3));
        assertEquals(-1, instance.testIndexOfByteType(new byte[] { 1, 2, 3, 4, 5 }, (byte) 9));
        assertEquals(2, instance.testIndexOfByteType(new byte[] { 1, 2, 3, 4, 5 }, (byte) 3));
    }

    @Test
    public void testByteIndexOf() {
        assertEquals(-1, instance.testIndexOfByte(null, (byte) 3));
        assertEquals(-1, instance.testIndexOfByte(new Byte[] { 1, 2, 3, 4, 5 }, (byte) 9));
        assertEquals(2, instance.testIndexOfByte(new Byte[] { 1, 2, 3, 4, 5 }, (byte) 3));
    }

    @Test
    public void testIndexOfShortType() {
        assertEquals(-1, instance.testIndexOfShortType(null, (short) 3));
        assertEquals(-1, instance.testIndexOfShortType(new short[] { 1, 2, 3, 4, 5 }, (short) 9));
        assertEquals(2, instance.testIndexOfShortType(new short[] { 1, 2, 3, 4, 5 }, (short) 3));
    }

    @Test
    public void testShortIndexOf() {
        assertEquals(-1, instance.testIndexOfShort(null, (short) 3));
        assertEquals(-1, instance.testIndexOfShort(new Short[] { 1, 2, 3, 4, 5 }, (short) 9));
        assertEquals(2, instance.testIndexOfShort(new Short[] { 1, 2, 3, 4, 5 }, (short) 3));
    }

    @Test
    public void testIndexOfCharType() {
        assertEquals(-1, instance.testIndexOfCharType(null, '3'));
        assertEquals(-1, instance.testIndexOfCharType(new char[] { '1', '2', '3', '4', '5' }, '9'));
        assertEquals(2, instance.testIndexOfCharType(new char[] { '1', '2', '3', '4', '5' }, '3'));
    }

    @Test
    public void testCharIndexOf() {
        assertEquals(-1, instance.testIndexOfChar(null, '3'));
        assertEquals(-1, instance.testIndexOfChar(new Character[] { '1', '2', '3', '4', '5' }, '9'));
        assertEquals(2, instance.testIndexOfChar(new Character[] { '1', '2', '3', '4', '5' }, '3'));
    }

    @Test
    public void testIndexOfFloatType() {
        assertEquals(-1, instance.testIndexOfFloatType(null, 3f));
        assertEquals(-1, instance.testIndexOfFloatType(new float[] { 1, 2, 3, 4, 5 }, 9f));
        assertEquals(2, instance.testIndexOfFloatType(new float[] { 1, 2, 3, 4, 5 }, 3f));
    }

    @Test
    public void testFloatIndexOf() {
        assertEquals(-1, instance.testIndexOfFloat(null, 3f));
        assertEquals(-1, instance.testIndexOfFloat(new Float[] { 1f, 2f, 3f, 4f, 5f }, 9f));
        assertEquals(2, instance.testIndexOfFloat(new Float[] { 1f, 2f, 3f, 4f, 5f }, 3f));
    }

    @Test
    public void testIndexOfDoubleType() {
        assertEquals(-1, instance.testIndexOfDoubleType(null, 3.3));
        assertEquals(-1, instance.testIndexOfDoubleType(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, 9.9));
        assertEquals(2, instance.testIndexOfDoubleType(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, 3.3));
    }

    @Test
    public void testDoubleIndexOf() {
        assertEquals(-1, instance.testIndexOfDouble(null, 3.3));
        assertEquals(-1, instance.testIndexOfDouble(new Double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, 9.9));
        assertEquals(2, instance.testIndexOfDouble(new Double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, 3.3));
    }

    @Test
    public void testIndexOfBoolType() {
        assertEquals(-1, instance.testIndexOfBoolType(null, false));
        assertEquals(-1, instance.testIndexOfBoolType(new boolean[] { true, true, true, true, true }, false));
        assertEquals(2, instance.testIndexOfBoolType(new boolean[] { true, true, false, true, true }, false));
    }

    @Test
    public void testBoolIndexOf() {
        assertEquals(-1, instance.testIndexOfBool(null, false));
        assertEquals(-1, instance.testIndexOfBool(new Boolean[] { true, true, true, true, true }, false));
        assertEquals(2, instance.testIndexOfBool(new Boolean[] { true, true, false, true, true }, false));
    }

    @Test
    public void testObjectNoNulls() {
        assertTrue(instance.testNoNullsObject(new Object[] { 1, 2, 3, 5 }));
        assertFalse(instance.testNoNullsObject(new Object[] { 1, null, 3, 5 }));
        assertFalse(instance.testNoNullsObject(new Object[] { 1, 2, 3, null }));
        assertFalse(instance.testNoNullsObject(new Object[] { null, 2, 3, 5 }));
        assertFalse(instance.testNoNullsObject(new Object[] { null }));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testError() {
        instance.testError("Ya oshibka, trololo :)");
    }

    @Test
    public void testDoubleFormat() {
        assertEquals("5.50", instance.formatDouble(5.5));
    }

    @Test
    public void testDoubleFormatWithFrm() {
        assertEquals("5.5000", instance.formatDoubleWithFrm(5.5, "#,####0.0000"));
    }

    @Test
    public void testStringArrIntersection() {
        String[] searchIn = new String[] { "abc", "def", "ghi", "jkl" };
        String[] searchFor = new String[] { "def", "jkl" };
        assertArrayEquals(searchFor, instance.testIntersectionStringArr(searchIn, searchFor));
    }

    @Test
    public void testMonthAbs() {
        int year = 2013;
        int month = 1;
        int date = 25;
        int hour = 15;
        int min = 3;
        Calendar c = Calendar.getInstance();
        Locale.setDefault(Locale.ENGLISH);

        c.set(year, month, date, hour, min);

        Date dateNow = c.getTime();
        assertEquals(24157, instance.testAbsMonth(dateNow));
    }

    @Test
    public void testMonthQuarter() {
        int year = 2013;
        int month = 1;
        int date = 25;
        int hour = 15;
        int min = 3;
        Calendar c = Calendar.getInstance();
        Locale.setDefault(Locale.ENGLISH);

        c.set(year, month, date, hour, min);

        Date dateNow = c.getTime();
        assertEquals(8052, instance.testAbsQuarter(dateNow));
    }

    @Test
    public void testDateDiff() {
        Calendar c = Calendar.getInstance();
        Locale.setDefault(Locale.ENGLISH);

        c.set(2013, 1, 25, 15, 3);
        Date date1 = c.getTime();
        c.set(2010, 1, 25, 15, 3);
        Date date2 = c.getTime();
        assertEquals(1096, instance.testDiffDate(date1, date2));
        assertEquals(-1096, instance.testDiffDate(date2, date1));
    }

    @Test
    public void testDayOfMonth() {
        Calendar c = Calendar.getInstance();
        Locale.setDefault(Locale.ENGLISH);

        c.set(2013, 1, 25, 15, 3);
        Date date1 = c.getTime();
        assertEquals(25, instance.testDayOfMonth(date1));
    }

    @Test
    public void testFirstDayOfQuarter() {
        Calendar c = Calendar.getInstance();
        c.set(2018, 6, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);

        Date date1 = c.getTime();

        assertEquals(date1, instance.testFirstDayOfQuarter(8074));
    }

    @Test
    public void testLastDayOfQuarter() {
        Calendar c = Calendar.getInstance();
        c.set(2018, 5, 30, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);

        Date date1 = c.getTime();

        assertEquals(date1, instance.testLastDayOfQuarter(8073));
    }

    @Test
    public void testLastDayOfMonth() {
        Calendar c = Calendar.getInstance();

        assertEquals(c.getActualMaximum(Calendar.DAY_OF_MONTH), instance.testLastDayOfMonth(c.getTime()));
    }

    @Test
    public void testGetMonth() {
        Calendar c = Calendar.getInstance();
        // set Dec 1
        c.set(2010, 11, 1, 0, 0, 0);
        assertEquals(11, instance.testGetMonth(c.getTime()));
        // set Jan 1
        c.set(2010, 0, 1, 0, 0, 0);
        assertEquals(0, instance.testGetMonth(c.getTime()));
    }

    @Test
    public void testMonthDiff() {
        Calendar cal = Calendar.getInstance();
        cal.set(2010, 3, 1);
        Date startDate = cal.getTime();
        cal.set(2011, 5, 1);
        Date endDate = cal.getTime();
        assertEquals(14, instance.testMonthDiff(endDate, startDate));

        cal.set(2012, 3, 10);
        startDate = cal.getTime();
        assertEquals(-10, instance.testMonthDiff(endDate, startDate));
    }

    @Test
    public void testYearDiff() {
        Calendar cal = Calendar.getInstance();
        cal.set(2001, 10, 1);
        Date startDate = cal.getTime();
        cal.set(2013, 10, 1);
        Date endDate = cal.getTime();
        assertEquals(12, instance.testYearDiff(endDate, startDate));

        cal.set(2015, 10, 10);
        startDate = cal.getTime();
        assertEquals(-2, instance.testYearDiff(endDate, startDate));
    }

    @Test
    public void testWeekDiff() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 1, 1);
        Date startDate = cal.getTime();
        cal.set(2013, 2, 1);
        Date endDate = cal.getTime();
        assertEquals(4, instance.testWeekDiff(endDate, startDate));

        cal.set(2013, 3, 10);
        startDate = cal.getTime();
        assertEquals(-5, instance.testWeekDiff(endDate, startDate));
    }

    @Test
    public void testQuarter() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 11, 1);
        Date date = cal.getTime();
        assertEquals(3, instance.testQuarter(date));
        cal.set(2013, 0, 1);
        date = cal.getTime();
        assertEquals(0, instance.testQuarter(date));
    }

    @Test
    public void testYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 11, 1);
        Date date = cal.getTime();
        assertEquals(2013, instance.testYear(date));
    }

    @Test
    public void testDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 11, 9);
        Date date = cal.getTime();
        assertEquals(2, instance.testDayOfWeek(date));
    }

    @Test
    public void testDayOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 11, 1);
        cal.set(2013, 11, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date date = cal.getTime();
        assertEquals(365, instance.testDayOfYear(date));
    }

    @Test
    public void testWeekOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 0, 1);
        cal.set(2013, 11, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date date = cal.getTime();
        assertEquals(1, instance.testWeekOfYear(date));
    }

    @Test
    public void testWeekOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 8, 1);
        cal.set(2013, 11, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date date = cal.getTime();
        assertEquals(5, instance.testWeekOfMonth(date));

        cal.set(2013, 8, 1);
        date = cal.getTime();
        assertEquals(1, instance.testWeekOfMonth(date));
    }

    @Test
    public void testSecond() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 8, 1, 10, 49, 59);
        Date date = cal.getTime();
        assertEquals(59, instance.testSecond(date));

        cal.set(2013, 8, 1, 10, 49, 0);
        date = cal.getTime();
        assertEquals(0, instance.testSecond(date));
    }

    @Test
    public void testMinute() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 8, 1, 10, 49, 59);
        Date date = cal.getTime();
        assertEquals(49, instance.testMinute(date));

        cal.set(2013, 8, 1, 10, 0, 0);
        date = cal.getTime();
        assertEquals(0, instance.testMinute(date));
    }

    @Test
    public void testHour() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 8, 1, 13, 49, 59);
        Date date = cal.getTime();
        assertEquals(1, instance.testHour(date));

        cal.set(2013, 8, 1, 0, 0, 0);
        date = cal.getTime();
        assertEquals(0, instance.testHour(date));
    }

    @Test
    public void testHourOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 8, 1, 13, 49, 59);
        Date date = cal.getTime();
        assertEquals(13, instance.testHourOfDay(date));

        cal.set(2013, 8, 1, 0, 0, 0);
        date = cal.getTime();
        assertEquals(0, instance.testHourOfDay(date));
    }

    @Test
    public void testAmPm() {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 8, 1, 13, 49, 59);
        Date date = cal.getTime();
        assertEquals("PM", instance.testAmPm(date));

        cal.set(2013, 8, 1, 0, 0, 0);
        date = cal.getTime();
        assertEquals("AM", instance.testAmPm(date));
    }

    @Test
    public void testProductByteType() {
        byte[] inputArray = { (byte) 0, (byte) 9, (byte) 7 };
        assertEquals(0.0, instance.testByteTypeProduct(inputArray));
        inputArray = new byte[] { (byte) 1, (byte) 9, (byte) 7 };
        assertEquals(63.0, instance.testByteTypeProduct(inputArray));
    }

    @Test
    public void testProductShortType() {
        short[] inputArray = { (short) 0, (short) 9, (short) 7 };
        assertEquals(0.0, instance.testShortTypeProduct(inputArray));
        inputArray = new short[] { (short) 1, (short) 9, (short) 7 };
        assertEquals(63.0, instance.testShortTypeProduct(inputArray));
    }

    @Test
    public void testProductIntegerType() {
        int[] inputArray = { 0, 9, 7 };
        assertEquals(0.0, instance.testIntegerTypeProduct(inputArray));
        inputArray = new int[] { 1, 9, 7 };
        assertEquals(63.0, instance.testIntegerTypeProduct(inputArray));
    }

    @Test
    public void testProductLongType() {
        long[] inputArray = { (long) 0, (long) 9, (long) 7 };
        assertEquals(0.0, instance.testLongTypeProduct(inputArray));
        inputArray = new long[] { (long) 1, (long) 9, (long) 7 };
        assertEquals(63.0, instance.testLongTypeProduct(inputArray));
    }

    @Test
    public void testProductFloatType() {
        float[] inputArray = { (float) 0.0, (float) 9.1, (float) 7.2 };
        assertEquals(0.0, instance.testFloatTypeProduct(inputArray), 1e-15);
        inputArray = new float[] { (float) 1.0, (float) 9.1, (float) 7.9 };
        assertEquals(71.89, instance.testFloatTypeProduct(inputArray), 0.001f);
    }

    @Test
    public void testProductDoubleType() {
        double[] inputArray = { 0.0, 9.1, 7.2 };
        assertEquals(0.0, instance.testDoubleTypeProduct(inputArray));
        inputArray = new double[] { 1.0, 9.1, 7.9 };
        assertEquals(71.89, instance.testDoubleTypeProduct(inputArray));
    }

    @Test
    public void testProductByte() {
        Byte[] inputArray = { 0, 9, 7 };
        assertEquals(0.0, instance.testByteProduct(inputArray));
        inputArray = new Byte[] { 1, 9, 7 };
        assertEquals(63.0, instance.testByteProduct(inputArray));
        inputArray = new Byte[] { null, 9, 7 };
        assertEquals(63.0, instance.testByteProduct(inputArray));
    }

    @Test
    public void testProductShort() {
        Short[] inputArray = { 0, 9, 7 };
        assertEquals(0.0, instance.testShortProduct(inputArray));
        inputArray = new Short[] { 1, 9, 7 };
        assertEquals(63.0, instance.testShortProduct(inputArray));
        inputArray = new Short[] { null, 9, 7 };
        assertEquals(63.0, instance.testShortProduct(inputArray));
    }

    @Test
    public void testProductInteger() {
        Integer[] inputArray = { 0, 9, 7 };
        assertEquals(0.0, instance.testIntegerProduct(inputArray));
        inputArray = new Integer[] { 1, 9, 7 };
        assertEquals(63.0, instance.testIntegerProduct(inputArray));
        inputArray = new Integer[] { null, 9, 7 };
        assertEquals(63.0, instance.testIntegerProduct(inputArray));
    }

    @Test
    public void testProductLong() {
        Long[] inputArray = { (long) 0, (long) 9, (long) 7 };
        assertEquals(0.0, instance.testLongProduct(inputArray));
        inputArray = new Long[] { (long) 1, (long) 9, (long) 7 };
        assertEquals(63.0, instance.testLongProduct(inputArray));
        inputArray = new Long[] { null, (long) 9, (long) 7 };
        assertEquals(63.0, instance.testLongProduct(inputArray));
    }

    @Test
    public void testProductFloat() {
        Float[] inputArray = { (float) 0.0, (float) 9.1, (float) 7.2 };
        assertEquals(0.0, instance.testFloatProduct(inputArray), 0.001f);
        inputArray = new Float[] { (float) 1.0, (float) 9.1, (float) 7.9 };
        assertEquals(71.89, instance.testFloatProduct(inputArray), 0.001f);
        inputArray = new Float[] { null, (float) 9.0, (float) 7.4 };
        assertEquals(66.6, instance.testFloatProduct(inputArray), 0.001f);
    }

    @Test
    public void testProductDouble() {
        Double[] inputArray = { (double) 0.0, (double) 9.1, (double) 7.2 };
        assertEquals((double) 0.0, instance.testDoubleProduct(inputArray));
        inputArray = new Double[] { (double) 1.0, (double) 9.1, (double) 7.9 };
        assertEquals((double) 71.89, instance.testDoubleProduct(inputArray));
        inputArray = new Double[] { null, (double) 9.1, (double) 7.7 };
        assertEquals(70.07, instance.testDoubleProduct(inputArray));
    }

    @Test
    public void testProductBigInteger() {
        BigInteger[] inputArray = { BigInteger.valueOf(0), BigInteger.valueOf(9), BigInteger.valueOf(7) };
        assertEquals(BigInteger.valueOf(0), instance.testBigIntegerProduct(inputArray));
        inputArray = new BigInteger[] { BigInteger.valueOf(1), BigInteger.valueOf(9), BigInteger.valueOf(7) };
        assertEquals(BigInteger.valueOf(63), instance.testBigIntegerProduct(inputArray));
        inputArray = new BigInteger[] { null, BigInteger.valueOf(9), BigInteger.valueOf(7) };
        assertEquals(BigInteger.valueOf(63), instance.testBigIntegerProduct(inputArray));
    }

    @Test
    public void testProductBigDecimal() {
        BigDecimal[] inputArray = { BigDecimal.valueOf(0), BigDecimal.valueOf(9), BigDecimal.valueOf(7) };
        assertEquals(BigDecimal.valueOf(0), instance.testBigDecimalProduct(inputArray));
        inputArray = new BigDecimal[] { BigDecimal.valueOf(1), BigDecimal.valueOf(9), BigDecimal.valueOf(7) };
        assertEquals(BigDecimal.valueOf(63), instance.testBigDecimalProduct(inputArray));
        inputArray = new BigDecimal[] { null, BigDecimal.valueOf(9), BigDecimal.valueOf(7) };
        assertEquals(BigDecimal.valueOf(63), instance.testBigDecimalProduct(inputArray));
    }

    @Test
    public void testRoundDoubleType() {
        assertEquals(6l, instance.testDoubleTypeRound(5.67));
        assertEquals(0l, instance.testDoubleTypeRound(0.01));
        assertEquals(6l, instance.testDoubleTypeRound(5.99));
        assertEquals(5l, instance.testDoubleTypeRound(5.3));
        assertEquals(-6l, instance.testDoubleTypeRound(-5.9));
    }

    @Test
    public void testRoundFloatType() {
        assertEquals(6, instance.testFloatTypeRound(5.67f));
        assertEquals(0, instance.testFloatTypeRound(0.01f));
        assertEquals(6, instance.testFloatTypeRound(5.99f));
        assertEquals(5, instance.testFloatTypeRound(5.3f));
        assertEquals(-6, instance.testFloatTypeRound(-5.9f));
    }

    @Test
    public void testRoundScaleDoubleType() {
        assertEquals(5.7, instance.testDoubleTypeRoundScale(5.67, 1));
        assertEquals(0.0, instance.testDoubleTypeRoundScale(0.0, 4));
        assertEquals(5.99, instance.testDoubleTypeRoundScale(5.99, 2));
        assertEquals(5.3, instance.testDoubleTypeRoundScale(5.3, 2));
        assertEquals(-5.9, instance.testDoubleTypeRoundScale(-5.9, 3));
    }

    @Test
    public void testRoundScaleFloatType() {
        assertEquals(5.7f, instance.testFloatTypeRoundScale(5.67f, 1));
        assertEquals(0.0f, instance.testFloatTypeRoundScale(0.0f, 4));
        assertEquals(5.99f, instance.testFloatTypeRoundScale(5.99f, 2));
        assertEquals(5.3f, instance.testFloatTypeRoundScale(5.3f, 2));
        assertEquals(-5.9f, instance.testFloatTypeRoundScale(-5.9f, 3));
    }

    @Test
    public void testRoundScaleRoundMethodDoubleType() {
        assertEquals(5.7, instance.testDoubleTypeRoundScaleRoundMethod(5.67, 1, 0));
        assertEquals(0.0, instance.testDoubleTypeRoundScaleRoundMethod(0.0, 4, 1));
        assertEquals(5.99, instance.testDoubleTypeRoundScaleRoundMethod(5.99, 2, 2));
        assertEquals(5.3, instance.testDoubleTypeRoundScaleRoundMethod(5.3, 2, 3));
        assertEquals(-5.9, instance.testDoubleTypeRoundScaleRoundMethod(-5.9, 3, 4));
        assertEquals(5.7, instance.testDoubleTypeRoundScaleRoundMethod(5.67, 1, 5));
        assertEquals(0.0, instance.testDoubleTypeRoundScaleRoundMethod(0.0, 4, 6));
        assertEquals(5.99, instance.testDoubleTypeRoundScaleRoundMethod(5.99, 2, 7));
    }

    @Test
    public void testRoundScaleRoundMethodFloatType() {
        assertEquals(5.7f, instance.testFloatTypeRoundScaleRoundMethod(5.67f, 1, 0));
        assertEquals(0.0f, instance.testFloatTypeRoundScaleRoundMethod(-0.0000999f, 4, 1));
        assertEquals(6.0f, instance.testFloatTypeRoundScaleRoundMethod(5.991f, 2, 2));
        assertEquals(5.29f, instance.testFloatTypeRoundScaleRoundMethod(5.299f, 2, 3));
        assertEquals(-5.9f, instance.testFloatTypeRoundScaleRoundMethod(-5.9f, 3, 4));
        assertEquals(5.7f, instance.testFloatTypeRoundScaleRoundMethod(5.67f, 1, 5));
        assertEquals(0.0f, instance.testFloatTypeRoundScaleRoundMethod(0.0005f, 3, 6));
        assertEquals(5.99f, instance.testFloatTypeRoundScaleRoundMethod(5.99f, 2, 7));
    }

    @Test
    public void testRoundBigDecimal() {
        assertEquals(BigDecimal.valueOf(6), instance.testBigDecimalRound(BigDecimal.valueOf(5.67)));
        assertEquals(BigDecimal.valueOf(0), instance.testBigDecimalRound(BigDecimal.valueOf(0.01)));
        assertEquals(BigDecimal.valueOf(6), instance.testBigDecimalRound(BigDecimal.valueOf(5.99)));
        assertEquals(BigDecimal.valueOf(5), instance.testBigDecimalRound(BigDecimal.valueOf(5.3)));
        assertEquals(BigDecimal.valueOf(-6), instance.testBigDecimalRound(BigDecimal.valueOf(-5.9)));

        assertNull(instance.testBigDecimalRound(null));
    }

    @Test
    public void testRoundScaleBigDecimal() {
        assertEquals(BigDecimal.valueOf(5.7), instance.testBigDecimalScaleRound(BigDecimal.valueOf(5.67), 1));
        assertEquals(new BigDecimal("0.0100"), instance.testBigDecimalScaleRound(BigDecimal.valueOf(0.01), 4));
        assertEquals(BigDecimal.valueOf(5.99), instance.testBigDecimalScaleRound(BigDecimal.valueOf(5.99), 2));
        assertEquals(new BigDecimal("5.30"), instance.testBigDecimalScaleRound(BigDecimal.valueOf(5.3), 2));
        assertEquals(new BigDecimal("-5.900"), instance.testBigDecimalScaleRound(BigDecimal.valueOf(-5.9), 3));
    }

    @Test
    public void testRoundScaleRoundMethodBigDecimal() {
        assertEquals(BigDecimal.valueOf(5.7),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(5.67), 1, 0));
        assertEquals(new BigDecimal("0.0100"),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(0.01), 4, 1));
        assertEquals(BigDecimal.valueOf(5.99),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(5.99), 2, 2));
        assertEquals(new BigDecimal("5.30"),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(5.3), 2, 3));
        assertEquals(new BigDecimal("-5.900"),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(-5.9), 3, 4));
        assertEquals(BigDecimal.valueOf(5.7),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(5.67), 1, 5));
        assertEquals(new BigDecimal("0.0100"),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(0.01), 4, 6));
        assertEquals(BigDecimal.valueOf(5.99),
            instance.testBigDecimalScaleRoundRoundMethod(BigDecimal.valueOf(5.99), 2, 7));
    }

    @Test
    public void testRemoveNulls() {
        Integer[] inputArray = { 1, 3, 4, 5 };
        assertArrayEquals(inputArray, instance.testRemoveNulls(inputArray));
        inputArray = new Integer[] { 1, 3, 4, null };
        assertArrayEquals(new Integer[] { 1, 3, 4 }, instance.testRemoveNulls(inputArray));
        inputArray = new Integer[] { null, 3, 4, null };
        assertArrayEquals(new Integer[] { 3, 4 }, instance.testRemoveNulls(inputArray));
        inputArray = new Integer[] { null, null, null, null };
        assertArrayEquals(new Integer[] {}, instance.testRemoveNulls(inputArray));
    }

    @Test
    public void testAbsDouble() {
        assertEquals(5.5, instance.testDoubleAbs(-5.5));
        assertEquals(0.0, instance.testDoubleAbs(-0.0));
        assertEquals(0.0, instance.testDoubleAbs(0.0));
        assertEquals(5.5, instance.testDoubleAbs(5.5));
        assertEquals(5.9, instance.testDoubleAbs(-5.9));
    }

    @Test
    public void testAbsFloat() {
        assertEquals(5.5f, instance.testFloatAbs(-5.5f));
        assertEquals(0.0f, instance.testFloatAbs(-0.0f));
        assertEquals(0.0f, instance.testFloatAbs(0.0f));
        assertEquals(5.5f, instance.testFloatAbs(5.5f));
        assertEquals(5.9f, instance.testFloatAbs(-5.9f));
    }

    @Test
    public void testAbsLong() {
        assertEquals(5l, instance.testLongAbs(-5l));
        assertEquals(0l, instance.testLongAbs(-0l));
        assertEquals(0l, instance.testLongAbs(0l));
        assertEquals(5l, instance.testLongAbs(5l));
        assertEquals(5l, instance.testLongAbs(-5l));
    }

    @Test
    public void testAbsInteger() {
        assertEquals(5, instance.testIntegerAbs(-5));
        assertEquals(0, instance.testIntegerAbs(-0));
        assertEquals(0, instance.testIntegerAbs(0));
        assertEquals(5, instance.testIntegerAbs(5));
        assertEquals(5, instance.testIntegerAbs(-5));
    }

    @Test
    public void testAcos() {
        assertEquals(1.5707963267948966, instance.testAcos(0.0));
        assertEquals(0.0, instance.testAcos(1.0));
        assertEquals(1.0471975511965979, instance.testAcos(0.5));
        assertEquals(1.5707963267948966, instance.testAcos(-0.0));
        assertEquals(2.0943951023931957, instance.testAcos(-0.5));
        assertEquals(3.141592653589793, instance.testAcos(-1.0));
    }

    @Test
    public void testCos() {
        assertEquals(Math.cos(0.0), instance.testCos(0.0));
        assertEquals(Math.cos(1.0), instance.testCos(1.0));
        assertEquals(Math.cos(0.5), instance.testCos(0.5));
        assertEquals(Math.cos(-0.0), instance.testCos(-0.0));
        assertEquals(Math.cos(-0.5), instance.testCos(-0.5));
        assertEquals(Math.cos(-1.0), instance.testCos(-1.0));
    }

    @Test
    public void testCosh() {
        assertEquals(Math.cosh(0.0), instance.testCosh(0.0));
        assertEquals(Math.cosh(1.0), instance.testCosh(1.0));
        assertEquals(Math.cosh(0.5), instance.testCosh(0.5));
        assertEquals(Math.cosh(-0.0), instance.testCosh(-0.0));
        assertEquals(Math.cosh(-0.5), instance.testCosh(-0.5));
        assertEquals(Math.cosh(-1.0), instance.testCosh(-1.0));
    }

    @Test
    public void testExp() {
        assertEquals(Math.exp(111), instance.testExp(111));
    }

    @Test
    public void testExpm1() {
        assertEquals(Math.expm1(111), instance.testExpm1(111));
    }

    @Test
    public void testAsin() {
        assertEquals(0.0, instance.testAsin(0.0));
        assertEquals(1.5707963267948966, instance.testAsin(1.0));
        assertEquals(0.5235987755982989, instance.testAsin(0.5));
        assertEquals(-0.0, instance.testAsin(-0.0));
        assertEquals(-0.5235987755982989, instance.testAsin(-0.5));
        assertEquals(-1.5707963267948966, instance.testAsin(-1.0));
    }

    @Test
    public void testAtan() {
        assertEquals(0.0, instance.testAtan(0.0));
        assertEquals(0.7853981633974483, instance.testAtan(1.0));
        assertEquals(0.4636476090008061, instance.testAtan(0.5));
        assertEquals(-0.0, instance.testAtan(-0.0));
        assertEquals(-0.4636476090008061, instance.testAtan(-0.5));
        assertEquals(-0.7853981633974483, instance.testAtan(-1.0));
    }

    @Test
    public void testAtan2() {
        assertEquals(0.0, instance.testAtan2(0.0, 0.0));
        assertEquals(0.0, instance.testAtan2(0.0, 1.0));
        assertEquals(1.5707963267948966, instance.testAtan2(1.0, 0.0));
        assertEquals(-1.5707963267948966, instance.testAtan2(-1.0, 0.0));
        assertEquals(1.5707963267948966, instance.testAtan2(1.0, -0.0));
        assertEquals(-1.5707963267948966, instance.testAtan2(-1.0, -0.0));
        assertEquals(-0.0, instance.testAtan2(-0.0, 1.0));
        assertEquals(3.141592653589793, instance.testAtan2(0.0, -1.0));
        assertEquals(-3.141592653589793, instance.testAtan2(-0.0, -1.0));
    }

    @Test
    public void testCbrt() {
        assertEquals(0.0, instance.testCbrt(0.0));
        assertEquals(1.0, instance.testCbrt(1.0));
        assertEquals(1.2599210498948732, instance.testCbrt(2.0));
        assertEquals(-1.0, instance.testCbrt(-1.0));
    }

    @Test
    public void testCeil() {
        assertEquals(0.0, instance.testCeil(0.0));
        assertEquals(2.0, instance.testCeil(1.3));
        assertEquals(-0.0, instance.testCeil(-0.8));
        assertEquals(-1.0, instance.testCeil(-1.3));
        assertEquals(100.0, instance.testCeil(99.99));
    }

    @Test
    public void testCopySignDouble() {
        assertEquals(0.0, instance.testDoubleCopySign(0.0, 1.0));
        assertEquals(1.0, instance.testDoubleCopySign(1.0, 1.0));
        assertEquals(1.0, instance.testDoubleCopySign(1.0, 2.0));
        assertEquals(2.4, instance.testDoubleCopySign(2.4, 1.0));
        assertEquals(2.4, instance.testDoubleCopySign(2.4, 0.2));
        assertEquals(2.4, instance.testDoubleCopySign(-2.4, 0.2));
        assertEquals(-2.4, instance.testDoubleCopySign(-2.4, -0.2));
    }

    @Test
    public void testCopySignFloat() {
        assertEquals(0.0f, instance.testFloatCopySign(0.0f, 1.0f));
        assertEquals(1.0f, instance.testFloatCopySign(1.0f, 1.0f));
        assertEquals(1.0f, instance.testFloatCopySign(1.0f, 2.0f));
        assertEquals(2.4f, instance.testFloatCopySign(2.4f, 1.0f));
        assertEquals(2.4f, instance.testFloatCopySign(2.4f, 0.2f));
        assertEquals(2.4f, instance.testFloatCopySign(-2.4f, 0.2f));
        assertEquals(-2.4f, instance.testFloatCopySign(-2.4f, -0.2f));
    }

    @Test
    public void testMaxIntegerType() {
        assertEquals(1, instance.testIntegerTypeMax(0, 1));
        assertEquals(1, instance.testIntegerTypeMax(1, 0));
        assertEquals(0, instance.testIntegerTypeMax(0, -1));
        assertEquals(7, instance.testIntegerTypeMax(7, 7));
        assertEquals(7, instance.testIntegerTypeMax(7, -7));
        assertEquals(-1, instance.testIntegerTypeMax(-9, -1));
    }

    @Test
    public void testMaxFloatType() {
        assertEquals(1.1f, instance.testFloatTypeMax(0.1f, 1.1f));
        assertEquals(1.1f, instance.testFloatTypeMax(1.1f, 0.1f));
        assertEquals(0.1f, instance.testFloatTypeMax(0.1f, -1.1f));
        assertEquals(7.1f, instance.testFloatTypeMax(7.1f, 7.1f));
        assertEquals(7.1f, instance.testFloatTypeMax(7.1f, -7.1f));
        assertEquals(-1.1f, instance.testFloatTypeMax(-9.9f, -1.1f));
    }

    @Test
    public void testMaxDoubleType() {
        assertEquals(1.1, instance.testDoubleTypeMax(0.1, 1.1));
        assertEquals(1.1, instance.testDoubleTypeMax(1.1, 0.1));
        assertEquals(0.1, instance.testDoubleTypeMax(0.1, -1.1));
        assertEquals(7.1, instance.testDoubleTypeMax(7.1, 7.1));
        assertEquals(7.1, instance.testDoubleTypeMax(7.1, -7.1));
        assertEquals(-1.1, instance.testDoubleTypeMax(-9.9, -1.1));
    }

    @Test
    public void testMaxLongType() {
        assertEquals(1l, instance.testLongTypeMax(0l, 1l));
        assertEquals(1l, instance.testLongTypeMax(1l, 0l));
        assertEquals(0l, instance.testLongTypeMax(0l, -1l));
        assertEquals(7l, instance.testLongTypeMax(7l, 7l));
        assertEquals(7l, instance.testLongTypeMax(7l, -7l));
        assertEquals(-1l, instance.testLongTypeMax(-9l, -1l));
    }

    @Test
    public void testMaxInteger() {
        assertEquals(Integer.valueOf(1), instance.testIntegerMax(Integer.valueOf(0), Integer.valueOf(1)));
        assertEquals(Integer.valueOf(1), instance.testIntegerMax(Integer.valueOf(1), Integer.valueOf(0)));
        assertEquals(Integer.valueOf(0), instance.testIntegerMax(Integer.valueOf(0), Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(7), instance.testIntegerMax(Integer.valueOf(7), Integer.valueOf(7)));
        assertEquals(Integer.valueOf(7), instance.testIntegerMax(Integer.valueOf(7), Integer.valueOf(-7)));
        assertEquals(Integer.valueOf(-1), instance.testIntegerMax(Integer.valueOf(-9), Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(-1), instance.testIntegerMax(null, Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(-9), instance.testIntegerMax(Integer.valueOf(-9), null));
    }

    @Test
    public void testMaxByte() {
        assertEquals(Byte.valueOf((byte) 1), instance.testByteMax(Byte.valueOf((byte) 0), Byte.valueOf((byte) 1)));
        assertEquals(Byte.valueOf((byte) 1), instance.testByteMax(Byte.valueOf((byte) 1), Byte.valueOf((byte) 0)));
        assertEquals(Byte.valueOf((byte) 0), instance.testByteMax(Byte.valueOf((byte) 0), Byte.valueOf((byte) -1)));
        assertEquals(Byte.valueOf((byte) 7), instance.testByteMax(Byte.valueOf((byte) 7), Byte.valueOf((byte) 7)));
        assertEquals(Byte.valueOf((byte) 7), instance.testByteMax(Byte.valueOf((byte) 7), Byte.valueOf((byte) -7)));
        assertEquals(Byte.valueOf((byte) -1), instance.testByteMax(Byte.valueOf((byte) -9), Byte.valueOf((byte) -1)));
        assertEquals(Byte.valueOf((byte) -1), instance.testByteMax(null, Byte.valueOf((byte) -1)));
        assertEquals(Byte.valueOf((byte) -9), instance.testByteMax(Byte.valueOf((byte) -9), null));
    }

    @Test
    public void testMaxShort() {
        assertEquals(Short.valueOf((short) 1),
            instance.testShortMax(Short.valueOf((short) 0), Short.valueOf((short) 1)));
        assertEquals(Short.valueOf((short) 1),
            instance.testShortMax(Short.valueOf((short) 1), Short.valueOf((short) 0)));
        assertEquals(Short.valueOf((short) 0),
            instance.testShortMax(Short.valueOf((short) 0), Short.valueOf((short) -1)));
        assertEquals(Short.valueOf((short) 7),
            instance.testShortMax(Short.valueOf((short) 7), Short.valueOf((short) 7)));
        assertEquals(Short.valueOf((short) 7),
            instance.testShortMax(Short.valueOf((short) 7), Short.valueOf((short) -7)));
        assertEquals(Short.valueOf((short) -1),
            instance.testShortMax(Short.valueOf((short) -9), Short.valueOf((short) -1)));
        assertEquals(Short.valueOf((short) -1), instance.testShortMax(null, Short.valueOf((short) -1)));
        assertEquals(Short.valueOf((short) -9), instance.testShortMax(Short.valueOf((short) -9), null));
    }

    @Test
    public void testMaxFloat() {
        assertEquals(Float.valueOf(1.1f), instance.testFloatMax(Float.valueOf(0.1f), Float.valueOf(1.1f)));
        assertEquals(Float.valueOf(1.1f), instance.testFloatMax(Float.valueOf(1.1f), Float.valueOf(0.1f)));
        assertEquals(Float.valueOf(0.1f), instance.testFloatMax(Float.valueOf(0.1f), Float.valueOf(-1.1f)));
        assertEquals(Float.valueOf(7.1f), instance.testFloatMax(Float.valueOf(7.1f), Float.valueOf(7.1f)));
        assertEquals(Float.valueOf(7.1f), instance.testFloatMax(Float.valueOf(7.1f), Float.valueOf(-7.1f)));
        assertEquals(Float.valueOf(-1.1f), instance.testFloatMax(Float.valueOf(-9.1f), Float.valueOf(-1.1f)));
        assertEquals(Float.valueOf(-1.1f), instance.testFloatMax(null, Float.valueOf(-1.1f)));
        assertEquals(Float.valueOf(-9.1f), instance.testFloatMax(Float.valueOf(-9.1f), null));
    }

    @Test
    public void testMaxDouble() {
        assertEquals(Double.valueOf(1.1), instance.testDoubleMax(Double.valueOf(0.1), Double.valueOf(1.1)));
        assertEquals(Double.valueOf(1.1), instance.testDoubleMax(Double.valueOf(1.1), Double.valueOf(0.1)));
        assertEquals(Double.valueOf(0.1), instance.testDoubleMax(Double.valueOf(0.1), Double.valueOf(-1.1)));
        assertEquals(Double.valueOf(7.1), instance.testDoubleMax(Double.valueOf(7.1), Double.valueOf(7.1)));
        assertEquals(Double.valueOf(7.1), instance.testDoubleMax(Double.valueOf(7.1), Double.valueOf(-7.1)));
        assertEquals(Double.valueOf(-1.1), instance.testDoubleMax(Double.valueOf(-9.1), Double.valueOf(-1.1)));
        assertEquals(Double.valueOf(-1.1), instance.testDoubleMax(null, Double.valueOf(-1.1)));
        assertEquals(Double.valueOf(-9.1), instance.testDoubleMax(Double.valueOf(-9.1), null));
    }

    @Test
    public void testMaxLong() {
        assertEquals(Long.valueOf(1), instance.testLongMax(Long.valueOf(0), Long.valueOf(1)));
        assertEquals(Long.valueOf(1), instance.testLongMax(Long.valueOf(1), Long.valueOf(0)));
        assertEquals(Long.valueOf(0), instance.testLongMax(Long.valueOf(0), Long.valueOf(-1)));
        assertEquals(Long.valueOf(7), instance.testLongMax(Long.valueOf(7), Long.valueOf(7)));
        assertEquals(Long.valueOf(7), instance.testLongMax(Long.valueOf(7), Long.valueOf(-7)));
        assertEquals(Long.valueOf(-1), instance.testLongMax(Long.valueOf(-9), Long.valueOf(-1)));
        assertEquals(Long.valueOf(-1), instance.testLongMax(null, Long.valueOf(-1)));
        assertEquals(Long.valueOf(-9), instance.testLongMax(Long.valueOf(-9), null));
    }

    @Test
    public void testMaxBigInteger() {
        assertEquals(BigInteger.valueOf(1), instance.testBigIntegerMax(BigInteger.valueOf(0), BigInteger.valueOf(1)));
        assertEquals(BigInteger.valueOf(1), instance.testBigIntegerMax(BigInteger.valueOf(1), BigInteger.valueOf(0)));
        assertEquals(BigInteger.valueOf(0), instance.testBigIntegerMax(BigInteger.valueOf(0), BigInteger.valueOf(-1)));
        assertEquals(BigInteger.valueOf(7), instance.testBigIntegerMax(BigInteger.valueOf(7), BigInteger.valueOf(7)));
        assertEquals(BigInteger.valueOf(7), instance.testBigIntegerMax(BigInteger.valueOf(7), BigInteger.valueOf(-7)));
        assertEquals(BigInteger.valueOf(-1),
            instance.testBigIntegerMax(BigInteger.valueOf(-9), BigInteger.valueOf(-1)));
        assertEquals(BigInteger.valueOf(-9), instance.testBigIntegerMax(BigInteger.valueOf(-9), null));
        assertEquals(BigInteger.valueOf(-1), instance.testBigIntegerMax(null, BigInteger.valueOf(-1)));
    }

    @Test
    public void testMaxBigDecimal() {
        assertEquals(BigDecimal.valueOf(1.1),
            instance.testBigDecimalMax(BigDecimal.valueOf(0.1), BigDecimal.valueOf(1.1)));
        assertEquals(BigDecimal.valueOf(1.1),
            instance.testBigDecimalMax(BigDecimal.valueOf(1.1), BigDecimal.valueOf(0.1)));
        assertEquals(BigDecimal.valueOf(0.1),
            instance.testBigDecimalMax(BigDecimal.valueOf(0.1), BigDecimal.valueOf(-1.1)));
        assertEquals(BigDecimal.valueOf(7.1),
            instance.testBigDecimalMax(BigDecimal.valueOf(7.1), BigDecimal.valueOf(7.1)));
        assertEquals(BigDecimal.valueOf(7.1),
            instance.testBigDecimalMax(BigDecimal.valueOf(7.1), BigDecimal.valueOf(-7.1)));
        assertEquals(BigDecimal.valueOf(-1.1),
            instance.testBigDecimalMax(BigDecimal.valueOf(-9.1), BigDecimal.valueOf(-1.1)));
        assertEquals(BigDecimal.valueOf(-1.1), instance.testBigDecimalMax(null, BigDecimal.valueOf(-1.1)));
        assertEquals(BigDecimal.valueOf(-9.1), instance.testBigDecimalMax(BigDecimal.valueOf(-9.1), null));
    }

    @Test
    public void testMinIntegerType() {
        assertEquals(0, instance.testIntegerTypeMin(0, 1));
        assertEquals(0, instance.testIntegerTypeMin(1, 0));
        assertEquals(-1, instance.testIntegerTypeMin(0, -1));
        assertEquals(7, instance.testIntegerTypeMin(7, 7));
        assertEquals(-7, instance.testIntegerTypeMin(7, -7));
        assertEquals(-9, instance.testIntegerTypeMin(-9, -1));
    }

    @Test
    public void testMinFloatType() {
        assertEquals(0.1f, instance.testFloatTypeMin(0.1f, 1.1f));
        assertEquals(0.1f, instance.testFloatTypeMin(1.1f, 0.1f));
        assertEquals(-1.1f, instance.testFloatTypeMin(0.1f, -1.1f));
        assertEquals(7.1f, instance.testFloatTypeMin(7.1f, 7.1f));
        assertEquals(-7.1f, instance.testFloatTypeMin(7.1f, -7.1f));
        assertEquals(-9.9f, instance.testFloatTypeMin(-9.9f, -1.1f));
    }

    @Test
    public void testMinDoubleType() {
        assertEquals(0.1, instance.testDoubleTypeMin(0.1, 1.1));
        assertEquals(0.1, instance.testDoubleTypeMin(1.1, 0.1));
        assertEquals(-1.1, instance.testDoubleTypeMin(0.1, -1.1));
        assertEquals(7.1, instance.testDoubleTypeMin(7.1, 7.1));
        assertEquals(-7.1, instance.testDoubleTypeMin(7.1, -7.1));
        assertEquals(-9.9, instance.testDoubleTypeMin(-9.9, -1.1));
    }

    @Test
    public void testMinLongType() {
        assertEquals(0l, instance.testLongTypeMin(0l, 1l));
        assertEquals(0l, instance.testLongTypeMin(1l, 0l));
        assertEquals(-1l, instance.testLongTypeMin(0l, -1l));
        assertEquals(7l, instance.testLongTypeMin(7l, 7l));
        assertEquals(-7l, instance.testLongTypeMin(7l, -7l));
        assertEquals(-9l, instance.testLongTypeMin(-9l, -1l));
    }

    @Test
    public void testMinInteger() {
        assertEquals(Integer.valueOf(0), instance.testIntegerMin(Integer.valueOf(0), Integer.valueOf(1)));
        assertEquals(Integer.valueOf(0), instance.testIntegerMin(Integer.valueOf(1), Integer.valueOf(0)));
        assertEquals(Integer.valueOf(-1), instance.testIntegerMin(Integer.valueOf(0), Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(7), instance.testIntegerMin(Integer.valueOf(7), Integer.valueOf(7)));
        assertEquals(Integer.valueOf(-7), instance.testIntegerMin(Integer.valueOf(7), Integer.valueOf(-7)));
        assertEquals(Integer.valueOf(-9), instance.testIntegerMin(Integer.valueOf(-9), Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(-1), instance.testIntegerMin(null, Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(-9), instance.testIntegerMin(Integer.valueOf(-9), null));
    }

    @Test
    public void testMinFloat() {
        assertEquals(Float.valueOf(0.1f), instance.testFloatMin(Float.valueOf(0.1f), Float.valueOf(1.1f)));
        assertEquals(Float.valueOf(0.1f), instance.testFloatMin(Float.valueOf(1.1f), Float.valueOf(0.1f)));
        assertEquals(Float.valueOf(-1.1f), instance.testFloatMin(Float.valueOf(0.1f), Float.valueOf(-1.1f)));
        assertEquals(Float.valueOf(7.1f), instance.testFloatMin(Float.valueOf(7.1f), Float.valueOf(7.1f)));
        assertEquals(Float.valueOf(-7.1f), instance.testFloatMin(Float.valueOf(7.1f), Float.valueOf(-7.1f)));
        assertEquals(Float.valueOf(-9.1f), instance.testFloatMin(Float.valueOf(-9.1f), Float.valueOf(-1.1f)));
        assertEquals(Float.valueOf(-1.1f), instance.testFloatMin(null, Float.valueOf(-1.1f)));
        assertEquals(Float.valueOf(-9.1f), instance.testFloatMin(Float.valueOf(-9.1f), null));
    }

    @Test
    public void testMinDouble() {
        assertEquals(Double.valueOf(0.1), instance.testDoubleMin(Double.valueOf(0.1), Double.valueOf(1.1)));
        assertEquals(Double.valueOf(0.1), instance.testDoubleMin(Double.valueOf(1.1), Double.valueOf(0.1)));
        assertEquals(Double.valueOf(-1.1), instance.testDoubleMin(Double.valueOf(0.1), Double.valueOf(-1.1)));
        assertEquals(Double.valueOf(7.1), instance.testDoubleMin(Double.valueOf(7.1), Double.valueOf(7.1)));
        assertEquals(Double.valueOf(-7.1), instance.testDoubleMin(Double.valueOf(7.1), Double.valueOf(-7.1)));
        assertEquals(Double.valueOf(-9.1), instance.testDoubleMin(Double.valueOf(-9.1), Double.valueOf(-1.1)));
        assertEquals(Double.valueOf(-1.1), instance.testDoubleMin(null, Double.valueOf(-1.1)));
        assertEquals(Double.valueOf(-9.1), instance.testDoubleMin(Double.valueOf(-9.1), null));
    }

    @Test
    public void testMinLong() {
        assertEquals(Long.valueOf(0), instance.testLongMin(Long.valueOf(0), Long.valueOf(1)));
        assertEquals(Long.valueOf(0), instance.testLongMin(Long.valueOf(1), Long.valueOf(0)));
        assertEquals(Long.valueOf(-1), instance.testLongMin(Long.valueOf(0), Long.valueOf(-1)));
        assertEquals(Long.valueOf(7), instance.testLongMin(Long.valueOf(7), Long.valueOf(7)));
        assertEquals(Long.valueOf(-7), instance.testLongMin(Long.valueOf(7), Long.valueOf(-7)));
        assertEquals(Long.valueOf(-9), instance.testLongMin(Long.valueOf(-9), Long.valueOf(-1)));
        assertEquals(Long.valueOf(-1), instance.testLongMin(null, Long.valueOf(-1)));
        assertEquals(Long.valueOf(-9), instance.testLongMin(Long.valueOf(-9), null));
    }

    @Test
    public void testMinByte() {
        assertEquals(Byte.valueOf((byte) 0), instance.testByteMin(Byte.valueOf((byte) 0), Byte.valueOf((byte) 1)));
        assertEquals(Byte.valueOf((byte) 0), instance.testByteMin(Byte.valueOf((byte) 1), Byte.valueOf((byte) 0)));
        assertEquals(Byte.valueOf((byte) -1), instance.testByteMin(Byte.valueOf((byte) 0), Byte.valueOf((byte) -1)));
        assertEquals(Byte.valueOf((byte) 7), instance.testByteMin(Byte.valueOf((byte) 7), Byte.valueOf((byte) 7)));
        assertEquals(Byte.valueOf((byte) -7), instance.testByteMin(Byte.valueOf((byte) 7), Byte.valueOf((byte) -7)));
        assertEquals(Byte.valueOf((byte) -9), instance.testByteMin(Byte.valueOf((byte) -9), Byte.valueOf((byte) -1)));
        assertEquals(Byte.valueOf((byte) -1), instance.testByteMin(null, Byte.valueOf((byte) -1)));
        assertEquals(Byte.valueOf((byte) -9), instance.testByteMin(Byte.valueOf((byte) -9), null));
    }

    @Test
    public void testMinShort() {
        assertEquals(Short.valueOf((short) 0),
            instance.testShortMin(Short.valueOf((short) 0), Short.valueOf((short) 1)));
        assertEquals(Short.valueOf((short) 0),
            instance.testShortMin(Short.valueOf((short) 1), Short.valueOf((short) 0)));
        assertEquals(Short.valueOf((short) -1),
            instance.testShortMin(Short.valueOf((short) 0), Short.valueOf((short) -1)));
        assertEquals(Short.valueOf((short) 7),
            instance.testShortMin(Short.valueOf((short) 7), Short.valueOf((short) 7)));
        assertEquals(Short.valueOf((short) -7),
            instance.testShortMin(Short.valueOf((short) 7), Short.valueOf((short) -7)));
        assertEquals(Short.valueOf((short) -9),
            instance.testShortMin(Short.valueOf((short) -9), Short.valueOf((short) -1)));
        assertEquals(Short.valueOf((short) -1), instance.testShortMin(null, Short.valueOf((byte) -1)));
        assertEquals(Short.valueOf((short) -9), instance.testShortMin(Short.valueOf((short) -9), null));
    }

    @Test
    public void testMinBigInteger() {
        assertEquals(BigInteger.valueOf(0), instance.testBigIntegerMin(BigInteger.valueOf(0), BigInteger.valueOf(1)));
        assertEquals(BigInteger.valueOf(0), instance.testBigIntegerMin(BigInteger.valueOf(1), BigInteger.valueOf(0)));
        assertEquals(BigInteger.valueOf(-1), instance.testBigIntegerMin(BigInteger.valueOf(0), BigInteger.valueOf(-1)));
        assertEquals(BigInteger.valueOf(7), instance.testBigIntegerMin(BigInteger.valueOf(7), BigInteger.valueOf(7)));
        assertEquals(BigInteger.valueOf(-7), instance.testBigIntegerMin(BigInteger.valueOf(7), BigInteger.valueOf(-7)));
        assertEquals(BigInteger.valueOf(-9),
            instance.testBigIntegerMin(BigInteger.valueOf(-9), BigInteger.valueOf(-1)));
        assertEquals(BigInteger.valueOf(-7), instance.testBigIntegerMin(null, BigInteger.valueOf(-7)));
        assertEquals(BigInteger.valueOf(7), instance.testBigIntegerMin(BigInteger.valueOf(7), null));
    }

    @Test
    public void testMinBigDecimal() {
        assertEquals(BigDecimal.valueOf(0.1),
            instance.testBigDecimalMin(BigDecimal.valueOf(0.1), BigDecimal.valueOf(1.1)));
        assertEquals(BigDecimal.valueOf(0.1),
            instance.testBigDecimalMin(BigDecimal.valueOf(1.1), BigDecimal.valueOf(0.1)));
        assertEquals(BigDecimal.valueOf(-1.1),
            instance.testBigDecimalMin(BigDecimal.valueOf(0.1), BigDecimal.valueOf(-1.1)));
        assertEquals(BigDecimal.valueOf(7.1),
            instance.testBigDecimalMin(BigDecimal.valueOf(7.1), BigDecimal.valueOf(7.1)));
        assertEquals(BigDecimal.valueOf(-7.1),
            instance.testBigDecimalMin(BigDecimal.valueOf(7.1), BigDecimal.valueOf(-7.1)));
        assertEquals(BigDecimal.valueOf(-9.1),
            instance.testBigDecimalMin(BigDecimal.valueOf(-9.1), BigDecimal.valueOf(-1.1)));
        assertEquals(BigDecimal.valueOf(-1.1), instance.testBigDecimalMin(null, BigDecimal.valueOf(-1.1)));
        assertEquals(BigDecimal.valueOf(-9.1), instance.testBigDecimalMin(BigDecimal.valueOf(-9.1), null));
    }

    @Test
    public void testToDegrees() {
        assertEquals(0.0, instance.testToDegrees(0.0));
        assertEquals(572.9577951308232, instance.testToDegrees(10.0));
    }

    @Test
    public void testToRadians() {
        assertEquals(0.0, instance.testToRadians(0.0));
        assertEquals(0.17453292519943295, instance.testToRadians(10.0));
    }

    @Test
    public void testAddBooleanType() {
        boolean[] inputArray = { true, true, true };
        boolean[] outputArray = instance.testBooleanTypeAdd(inputArray, false);
        assertTrue(ArrayUtils.contains(outputArray, false));
        outputArray = instance.testBooleanTypeAdd(null, false);
        assertEquals(false, outputArray[0]);
    }

    @Test
    public void testAddBooleanTypeInPosition() {
        boolean[] inputArray = { true, true, true };
        boolean[] outputArray = instance.testBooleanTypeAdd(inputArray, 2, false);
        assertEquals(false, outputArray[2]);
        outputArray = instance.testBooleanTypeAdd(null, 0, false);
        assertEquals(false, outputArray[0]);
    }

    @Test
    public void testAddBoolean() {
        Boolean[] inputArray = { true, true, true };
        Boolean[] outputArray = instance.testBooleanAdd(inputArray, false);
        assertTrue(ArrayUtils.contains(outputArray, false));
        outputArray = instance.testBooleanAdd(null, false);
        assertEquals(false, outputArray[0]);
    }

    @Test
    public void testAddBooleanInPosition() {
        Boolean[] inputArray = { true, true, true };
        Boolean[] outputArray = instance.testBooleanAdd(inputArray, 2, false);
        assertEquals(false, outputArray[2]);
        outputArray = instance.testBooleanAdd(null, 0, false);
        assertEquals(false, outputArray[0]);
    }

    @Test
    public void testAddByteType() {
        byte[] inputArray = { 0, 1, 2 };
        byte[] outputArray = instance.testByteTypeAdd(inputArray, (byte) 4);
        assertTrue(outputArray[3] == 4);
        outputArray = instance.testByteTypeAdd(null, (byte) 4);
        assertTrue(outputArray[0] == 4);
    }

    @Test
    public void testAddBypeTypeInPosition() {
        byte[] inputArray = { 0, 2, 3 };
        byte[] outputArray = instance.testByteTypeAdd(inputArray, 2, (byte) 1);
        assertEquals(1, outputArray[2]);
        outputArray = instance.testByteTypeAdd(null, 0, (byte) 4);
        assertTrue(outputArray[0] == 4);
    }

    @Test
    public void testAddByte() {
        Byte[] inputArray = { 0, 1, 2 };
        Byte[] outputArray = instance.testByteAdd(inputArray, (byte) 4);
        assertTrue(outputArray[3] == 4);
        outputArray = instance.testByteAdd(null, (byte) 4);
        assertTrue(outputArray[0] == 4);
    }

    @Test
    public void testAddBypeInPosition() {
        Byte[] inputArray = { 0, 2, 3 };
        Byte[] outputArray = instance.testByteAdd(inputArray, 2, (byte) 1);
        assertEquals(Byte.valueOf((byte) 1), outputArray[2]);
        outputArray = instance.testByteAdd(null, 0, (byte) 4);
        assertTrue(outputArray[0] == 4);
    }

    @Test
    public void testAddCharType() {
        char[] inputArray = { '0', '1', '2' };
        char[] outputArray = instance.testCharTypeAdd(inputArray, '4');
        assertEquals('4', outputArray[3]);
        outputArray = instance.testCharTypeAdd(null, '4');
        assertEquals('4', outputArray[0]);
    }

    @Test
    public void testAddCharTypeInPosition() {
        char[] inputArray = { '0', '2', '3' };
        char[] outputArray = instance.testCharTypeAdd(inputArray, 2, '1');
        assertEquals('1', outputArray[2]);
        outputArray = instance.testCharTypeAdd(null, 0, '4');
        assertEquals('4', outputArray[0]);
    }

    @Test
    public void testAddChar() {
        Character[] inputArray = { '0', '1', '2' };
        Character[] outputArray = instance.testCharAdd(inputArray, '4');
        assertEquals('4', (char) outputArray[3]);
        outputArray = instance.testCharAdd(null, '4');
        assertEquals('4', (char) outputArray[0]);
    }

    @Test
    public void testAddCharInPosition() {
        Character[] inputArray = { '0', '2', '3' };
        Character[] outputArray = instance.testCharAdd(inputArray, 2, '1');
        assertEquals('1', (char) outputArray[2]);
        outputArray = instance.testCharAdd(null, 0, '4');
        assertEquals('4', (char) outputArray[0]);
    }

    @Test
    public void testAddDoubleType() {
        double[] inputArray = { 0.1, 1.1, 2.1 };
        double[] outputArray = instance.testDoubleTypeAdd(inputArray, 4.1);
        assertEquals(4.1, outputArray[3], 0.0);
        outputArray = instance.testDoubleTypeAdd(null, 4.1);
        assertEquals(4.1, outputArray[0], 0.0);
    }

    @Test
    public void testAddDoubleTypeInPosition() {
        double[] inputArray = { 0.1, 2.1, 3.1 };
        double[] outputArray = instance.testDoubleTypeAdd(inputArray, 2, 1.1);
        assertEquals(1.1, outputArray[2], 0.0001);
        outputArray = instance.testDoubleTypeAdd(null, 0, 4.1);
        assertEquals(4.1, outputArray[0], 0.0);
    }

    @Test
    public void testAddDouble() {
        Double[] inputArray = { 0.1, 1.1, 2.1 };
        Double[] outputArray = instance.testDoubleAdd(inputArray, 4.1);
        assertEquals(4.1, outputArray[3], 0.0);
        outputArray = instance.testDoubleAdd(null, 4.1);
        assertEquals(4.1, outputArray[0], 0.0);
    }

    @Test
    public void testAddDoubleInPosition() {
        Double[] inputArray = { 0.1, 2.1, 3.1 };
        Double[] outputArray = instance.testDoubleAdd(inputArray, 2, 1.1);
        assertEquals(1.1, outputArray[2], 0.0001);
        outputArray = instance.testDoubleAdd(null, 0, 4.1);
        assertEquals(4.1, outputArray[0], 0.0);
    }

    @Test
    public void testAddFloatType() {
        float[] inputArray = { 0.1f, 1.1f, 2.1f };
        float[] outputArray = instance.testFloatTypeAdd(inputArray, 4.1f);
        assertEquals(4.1f, outputArray[3], 0.0);
        outputArray = instance.testFloatTypeAdd(null, 4.1f);
        assertEquals(4.1f, outputArray[0], 0.0);
    }

    @Test
    public void testAddFloatTypeInPosition() {
        float[] inputArray = { 0.1f, 2.1f, 3.1f };
        float[] outputArray = instance.testFloatTypeAdd(inputArray, 2, 1.1f);
        assertEquals(1.1f, outputArray[2], 0.0001);
        outputArray = instance.testFloatTypeAdd(null, 0, 4.1f);
        assertEquals(4.1f, outputArray[0], 0.0);
    }

    @Test
    public void testAddFloat() {
        Float[] inputArray = { 0.1f, 1.1f, 2.1f };
        Float[] outputArray = instance.testFloatAdd(inputArray, 4.1f);
        assertEquals(4.1f, outputArray[3], 0.0);
        outputArray = instance.testFloatAdd(null, 4.1f);
        assertEquals(4.1f, outputArray[0], 0.0);
    }

    @Test
    public void testAddFloatInPosition() {
        Float[] inputArray = { 0.1f, 2.1f, 3.1f };
        Float[] outputArray = instance.testFloatAdd(inputArray, 2, 1.1f);
        assertEquals(1.1f, outputArray[2], 0.0001);
        outputArray = instance.testFloatAdd(null, 0, 4.1f);
        assertEquals(4.1f, outputArray[0], 0.0);
    }

    @Test
    public void testAddIntegerType() {
        int[] inputArray = { 0, 1, 2 };
        int[] outputArray = instance.testIntegerTypeAdd(inputArray, 4);
        assertEquals(4, outputArray[3]);
        outputArray = instance.testIntegerTypeAdd(null, 4);
        assertEquals(4, outputArray[0]);
    }

    @Test
    public void testAddIntegerTypeInPosition() {
        int[] inputArray = { 0, 2, 3 };
        int[] outputArray = instance.testIntegerTypeAdd(inputArray, 2, 1);
        assertEquals(1, outputArray[2]);
        outputArray = instance.testIntegerTypeAdd(null, 0, 4);
        assertEquals(4, outputArray[0]);
    }

    @Test
    public void testAddInteger() {
        Integer[] inputArray = { 0, 1, 2 };
        Integer[] outputArray = instance.testIntegerAdd(inputArray, 4);
        assertEquals(4, (int) outputArray[3]);
        outputArray = instance.testIntegerAdd(null, 4);
        assertEquals(4, (int) outputArray[0]);
    }

    @Test
    public void testAddIntegerInPosition() {
        Integer[] inputArray = { 0, 2, 3 };
        Integer[] outputArray = instance.testIntegerAdd(inputArray, 2, 1);
        assertEquals(1, (int) outputArray[2]);
        outputArray = instance.testIntegerAdd(null, 0, 4);
        assertEquals(4, (int) outputArray[0]);
    }

    @Test
    public void testAddLongType() {
        long[] inputArray = { 0, 1, 2 };
        long[] outputArray = instance.testLongTypeAdd(inputArray, 4);
        assertEquals(4, outputArray[3]);
        outputArray = instance.testLongTypeAdd(null, 4);
        assertEquals(4, outputArray[0]);
    }

    @Test
    public void testAddLongTypeInPosition() {
        long[] inputArray = { 0, 2, 3 };
        long[] outputArray = instance.testLongTypeAdd(inputArray, 2, 1);
        assertEquals(1, outputArray[2]);
        outputArray = instance.testLongTypeAdd(null, 0, 4);
        assertEquals(4, outputArray[0]);
    }

    @Test
    public void testAddLong() {
        Long[] inputArray = { 0L, 1L, 2L };
        Long[] outputArray = instance.testLongAdd(inputArray, 4L);
        assertEquals(4, (long) outputArray[3]);
        outputArray = instance.testLongAdd(null, 4L);
        assertEquals(4, (long) outputArray[0]);
    }

    @Test
    public void testAddLongInPosition() {
        Long[] inputArray = { 0L, 2L, 3L };
        Long[] outputArray = instance.testLongAdd(inputArray, 2, 1L);
        assertEquals(1, (long) outputArray[2]);
        outputArray = instance.testLongAdd(null, 0, 4L);
        assertEquals(4, (long) outputArray[0]);
    }

    @Test
    public void testAddObject() {
        Object[] inputArray = { 0, 1, 2 };
        Object[] outputArray = instance.testObjectTypeAdd(inputArray, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[3]);
        outputArray = instance.testObjectTypeAdd(null, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[0]);
    }

    @Test
    public void testAddObjectInPosition() {
        Object[] inputArray = { 0, 2, 3 };
        Object[] outputArray = instance.testObjectTypeAdd(inputArray, 2, new ObjectValue(1));
        assertEquals(new ObjectValue(1), outputArray[2]);
        outputArray = instance.testObjectTypeAdd(null, 0, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[0]);
    }

    @Test
    public void testAddShortType() {
        short[] inputArray = { 0, 1, 2 };
        short[] outputArray = instance.testShortTypeAdd(inputArray, (short) 4);
        assertEquals(4, outputArray[3]);
        outputArray = instance.testShortTypeAdd(null, (short) 4);
        assertEquals(4, outputArray[0]);
    }

    @Test
    public void testAddShortTypeInPosition() {
        short[] inputArray = { 0, 2, 3 };
        short[] outputArray = instance.testShortTypeAdd(inputArray, 2, (short) 1);
        assertEquals(1, outputArray[2]);
        outputArray = instance.testShortTypeAdd(null, 0, (short) 4);
        assertEquals(4, outputArray[0]);
    }

    @Test
    public void testAddShort() {
        Short[] inputArray = { 0, 1, 2 };
        Short[] outputArray = instance.testShortAdd(inputArray, (short) 4);
        assertEquals(4, (short) outputArray[3]);
        outputArray = instance.testShortAdd(null, (short) 4);
        assertEquals(4, (short) outputArray[0]);
    }

    @Test
    public void testAddShortInPosition() {
        Short[] inputArray = { 0, 2, 3 };
        Short[] outputArray = instance.testShortAdd(inputArray, 2, (short) 1);
        assertEquals(1, (short) outputArray[2]);
        outputArray = instance.testShortAdd(null, 0, (short) 4);
        assertEquals(4, (short) outputArray[0]);
    }

    @Test
    public void testAddIgnoreNullsInPosition() {
        Object[] inputArray = { 0, 2, 3 };
        Object[] outputArray = instance.testObjectTypeAddIgnoreNulls(inputArray, 2, new ObjectValue(1));
        assertEquals(new ObjectValue(1), outputArray[2]);
        inputArray = new Object[] { 0, 2, 3 };
        outputArray = instance.testObjectTypeAddIgnoreNulls(inputArray, 2, null);
        assertArrayEquals(inputArray, outputArray);
        outputArray = instance.testObjectTypeAddIgnoreNulls(null, 0, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[0]);
    }

    @Test
    public void testAddIgnoreNullsObject() {
        Object[] inputArray = { 0, 1, 2 };
        Object[] outputArray = instance.testObjectTypeAddIgnoreNulls(inputArray, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[3]);
        inputArray = new Object[] { 0, 1, 2 };
        outputArray = instance.testObjectTypeAddIgnoreNulls(inputArray, null);
        assertArrayEquals(inputArray, outputArray);
        outputArray = instance.testObjectTypeAddIgnoreNulls(null, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[0]);
    }

    @Test
    public void testObjectAddIgnoreNullsInPosition() {
        Object[] inputArray = { 0, 2, 3 };
        Object[] outputArray = instance.testObjectAddIgnoreNulls(inputArray, 2, new ObjectValue(1));
        assertEquals(new ObjectValue(1), outputArray[2]);
        inputArray = new Object[] { 0, 2, 3 };
        outputArray = instance.testObjectAddIgnoreNulls(inputArray, 2, null);
        assertArrayEquals(inputArray, outputArray);
        outputArray = instance.testObjectAddIgnoreNulls(null, 0, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[0]);
    }

    @Test
    public void testObjectAddIgnoreNullsObject() {
        Object[] inputArray = { 0, 1, 2 };
        Object[] outputArray = instance.testObjectAddIgnoreNulls(inputArray, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[3]);
        inputArray = new Object[] { 0, 1, 2 };
        outputArray = instance.testObjectAddIgnoreNulls(inputArray, null);
        assertArrayEquals(inputArray, outputArray);
        outputArray = instance.testObjectAddIgnoreNulls(null, new ObjectValue(4));
        assertEquals(new ObjectValue(4), outputArray[0]);
    }

    @Test
    public void testAddAllByteType() {
        byte[] inputArray1 = { 0, 1, 2 };
        byte[] inputArray2 = { 3, 4, 5 };
        byte[] outputArray = instance.testByteTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testByteTypeAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testByteTypeAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllByte() {
        Byte[] inputArray1 = { 0, 1, 2 };
        Byte[] inputArray2 = { 3, 4, 5 };
        Byte[] outputArray = instance.testByteAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testByteAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testByteAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllCharType() {
        char[] inputArray1 = { '0', '1', '2' };
        char[] inputArray2 = { '3', '4', '5' };
        char[] outputArray = instance.testCharTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testCharTypeAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testCharTypeAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllChar() {
        Character[] inputArray1 = { '0', '1', '2' };
        Character[] inputArray2 = { '3', '4', '5' };
        Character[] outputArray = instance.testCharAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testCharAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testCharAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllDoubleType() {
        double[] inputArray1 = { 0.1, 1.1, 2.1 };
        double[] inputArray2 = { 3.1, 4.1, 5.1 };
        double[] outputArray = instance.testDoubleTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testDoubleTypeAddAll(null, inputArray2);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
        outputArray = instance.testDoubleTypeAddAll(inputArray2, null);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
    }

    @Test
    public void testAddAllDouble() {
        Double[] inputArray1 = { 0.1, 1.1, 2.1 };
        Double[] inputArray2 = { 3.1, 4.1, 5.1 };
        Double[] outputArray = instance.testDoubleAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testDoubleAddAll(null, inputArray2);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
        outputArray = instance.testDoubleAddAll(inputArray2, null);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
    }

    @Test
    public void testAddAllFloatType() {
        float[] inputArray1 = { 0.1f, 1.1f, 2.1f };
        float[] inputArray2 = { 3.1f, 4.1f, 5.1f };
        float[] outputArray = instance.testFloatTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testFloatTypeAddAll(null, inputArray2);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
        outputArray = instance.testFloatTypeAddAll(inputArray2, null);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
    }

    @Test
    public void testAddAllFloat() {
        Float[] inputArray1 = { 0.1f, 1.1f, 2.1f };
        Float[] inputArray2 = { 3.1f, 4.1f, 5.1f };
        Float[] outputArray = instance.testFloatAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testFloatAddAll(null, inputArray2);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
        outputArray = instance.testFloatAddAll(inputArray2, null);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
    }

    @Test
    public void testAddAllIntegerType() {
        int[] inputArray1 = { 0, 1, 2 };
        int[] inputArray2 = { 3, 4, 5 };
        int[] outputArray = instance.testIntegerTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testIntegerTypeAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testIntegerTypeAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllInteger() {
        Integer[] inputArray1 = { 0, 1, 2 };
        Integer[] inputArray2 = { 3, 4, 5 };
        Integer[] outputArray = instance.testIntegerAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testIntegerAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testIntegerAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllLongType() {
        long[] inputArray1 = { 0, 1, 2 };
        long[] inputArray2 = { 3, 4, 5 };
        long[] outputArray = instance.testLongTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testLongTypeAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testLongTypeAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllLong() {
        Long[] inputArray1 = { 0L, 1L, 2L };
        Long[] inputArray2 = { 3L, 4L, 5L };
        Long[] outputArray = instance.testLongAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testLongAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testLongAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllObject() {
        Object[] inputArray1 = { 0, 1, 2 };
        Object[] inputArray2 = { 3, 4, 5 };
        Object[] outputArray = instance.testObjectAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testObjectAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testObjectAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllShortType() {
        short[] inputArray1 = { 0, 1, 2 };
        short[] inputArray2 = { 3, 4, 5 };
        short[] outputArray = instance.testShortTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testShortTypeAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testShortTypeAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllShort() {
        Short[] inputArray1 = { 0, 1, 2 };
        Short[] inputArray2 = { 3, 4, 5 };
        Short[] outputArray = instance.testShortAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testShortAddAll(null, inputArray2);
        assertArrayEquals(inputArray2, outputArray);
        outputArray = instance.testShortAddAll(inputArray2, null);
        assertArrayEquals(inputArray2, outputArray);
    }

    @Test
    public void testAddAllBooleanType() {
        boolean[] inputArray1 = { true, false, true };
        boolean[] inputArray2 = { false, true, true };
        boolean[] outputArray = instance.testBooleanTypeAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testBooleanTypeAddAll(null, inputArray2);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
        outputArray = instance.testBooleanTypeAddAll(inputArray2, null);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
    }

    @Test
    public void testAddAllBoolean() {
        Boolean[] inputArray1 = { true, false, true };
        Boolean[] inputArray2 = { false, true, true };
        Boolean[] outputArray = instance.testBooleanAddAll(inputArray1, inputArray2);

        assertTrue(ArrayTool.containsAll(outputArray, inputArray1));
        assertTrue(ArrayTool.containsAll(outputArray, inputArray2));

        outputArray = instance.testBooleanAddAll(null, inputArray2);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
        outputArray = instance.testBooleanAddAll(inputArray2, null);
        assertTrue(ArrayTool.containsAll(inputArray2, outputArray));
    }

    @Test
    public void testRemoveBooleanType() {
        boolean[] inputArray1 = { true, false, true };
        boolean[] outputArray = instance.testBooleanTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new boolean[] { true, true }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testRemoveBooleanType_whenArrayOutOfIndex() {
        instance.testBooleanTypeRemove(new boolean[] { true, false, true }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testRemoveBooleanType_whenSourceIsNull() {
        instance.testBooleanTypeRemove(null, 1);
    }

    @Test
    public void testRemoveBoolean() {
        Boolean[] inputArray1 = { true, false, true };
        Boolean[] outputArray = instance.testBooleanRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Boolean[] { true, true }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testRemoveBoolean_whenArrayOutOfIndex() {
        instance.testBooleanRemove(new Boolean[] { true, false, true }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testRemoveBoolean_whenSourceIsNull() {
        instance.testBooleanRemove(null, 1);
    }

    @Test
    public void testRemoveByteType() {
        byte[] inputArray1 = { 0, 3, 5 };
        byte[] outputArray = instance.testByteTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new byte[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testByteTypeRemove_whenArrayOutOfIndex() {
        instance.testByteTypeRemove(new byte[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testByteTypeRemove_whenSourceIsNull() {
        instance.testByteTypeRemove(null, 1);
    }

    @Test
    public void testRemoveByte() {
        Byte[] inputArray1 = { 0, 3, 5 };
        Byte[] outputArray = instance.testByteRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Byte[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testByteRemove_whenArrayOutOfIndex() {
        instance.testByteRemove(new Byte[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testByteRemove_whenSourceIsNull() {
        instance.testByteRemove(null, 1);
    }

    @Test
    public void testRemoveCharType() {
        char[] inputArray1 = { '0', '5', '9' };
        char[] outputArray = instance.testCharTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new char[] { '0', '9' }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testCharTypeRemove_whenArrayOutOfIndex() {
        instance.testCharTypeRemove(new char[] { '1', '2', '3' }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testCharTypeRemove_whenSourceIsNull() {
        instance.testCharTypeRemove(null, 1);
    }

    @Test
    public void testRemoveChar() {
        Character[] inputArray1 = { '0', '5', '9' };
        Character[] outputArray = instance.testCharRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Character[] { '0', '9' }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testCharRemove_whenArrayOutOfIndex() {
        instance.testCharRemove(new Character[] { '1', '2', '3' }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testCharRemove_whenSourceIsNull() {
        instance.testCharRemove(null, 1);
    }

    @Test
    public void testRemoveDoubleType() {
        double[] inputArray1 = { 0.1, 3.1, 5.1 };
        double[] outputArray = instance.testDoubleTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new double[] { 0.1, 5.1 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testDoubleTypeRemove_whenArrayOutOfIndex() {
        instance.testDoubleTypeRemove(new double[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testDoubleTypeRemove_whenSourceIsNull() {
        instance.testDoubleTypeRemove(null, 1);
    }

    @Test
    public void testRemoveDouble() {
        Double[] inputArray1 = { 0.1, 3.1, 5.1 };
        Double[] outputArray = instance.testDoubleRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Double[] { 0.1, 5.1 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testDoubleRemove_whenArrayOutOfIndex() {
        instance.testDoubleRemove(new Double[] { 1., 2., 3. }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testDoubleRemove_whenSourceIsNull() {
        instance.testDoubleRemove(null, 1);
    }

    @Test
    public void testRemoveFloatType() {
        float[] inputArray1 = { 0.1f, 3.1f, 5.1f };
        float[] outputArray = instance.testFloatTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new float[] { 0.1f, 5.1f }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testFloatTypeRemove_whenArrayOutOfIndex() {
        instance.testFloatTypeRemove(new float[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testFloatTypeRemove_whenSourceIsNull() {
        instance.testFloatTypeRemove(null, 1);
    }

    @Test
    public void testRemoveFloat() {
        Float[] inputArray1 = { 0.1f, 3.1f, 5.1f };
        Float[] outputArray = instance.testFloatRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Float[] { 0.1f, 5.1f }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testFloatRemove_whenArrayOutOfIndex() {
        instance.testFloatRemove(new Float[] { 1f, 2f, 3f }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testFloatRemove_whenSourceIsNull() {
        instance.testFloatRemove(null, 1);
    }

    @Test
    public void testRemoveIntegerType() {
        int[] inputArray1 = { 0, 3, 5 };
        int[] outputArray = instance.testIntegerTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new int[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testIntegerTypeRemove_whenArrayOutOfIndex() {
        instance.testIntegerTypeRemove(new int[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testIntegerTypeRemove_whenSourceIsNull() {
        instance.testIntegerTypeRemove(null, 1);
    }

    @Test
    public void testRemoveInteger() {
        Integer[] inputArray1 = { 0, 3, 5 };
        Integer[] outputArray = instance.testIntegerRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Integer[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testIntegerRemove_whenArrayOutOfIndex() {
        instance.testIntegerRemove(new Integer[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testIntegerRemove_whenSourceIsNull() {
        instance.testIntegerRemove(null, 1);
    }

    @Test
    public void testRemoveLongType() {
        long[] inputArray1 = { 0, 3, 5 };
        long[] outputArray = instance.testLongTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new long[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testLongTypeRemove_whenArrayOutOfIndex() {
        instance.testLongTypeRemove(new long[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testLongTypeRemove_whenSourceIsNull() {
        instance.testLongTypeRemove(null, 1);
    }

    @Test
    public void testRemoveLong() {
        Long[] inputArray1 = { 0L, 3L, 5L };
        Long[] outputArray = instance.testLongRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Long[] { 0L, 5L }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testLongRemove_whenArrayOutOfIndex() {
        instance.testLongRemove(new Long[] { 1L, 2L, 3L }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testLongRemove_whenSourceIsNull() {
        instance.testLongRemove(null, 1);
    }

    @Test
    public void testRemoveObjectType() {
        Object[] inputArray1 = { 0, 3, 5 };
        Object[] outputArray = instance.testObjectTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Object[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testObjectTypeRemove_whenArrayOutOfIndex() {
        instance.testObjectTypeRemove(new Object[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testObjectTypeRemove_whenSourceIsNull() {
        instance.testObjectTypeRemove(null, 1);
    }

    @Test
    public void testRemoveShortType() {
        short[] inputArray1 = { 0, 3, 5 };
        short[] outputArray = instance.testShortTypeRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new short[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testShortTypeRemove_whenArrayOutOfIndex() {
        instance.testShortTypeRemove(new short[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testShortTypeRemove_whenSourceIsNull() {
        instance.testShortTypeRemove(null, 1);
    }

    @Test
    public void testRemoveShort() {
        Short[] inputArray1 = { 0, 3, 5 };
        Short[] outputArray = instance.testShortRemove(inputArray1, 1);
        assertTrue(Arrays.equals(new Short[] { 0, 5 }, outputArray));
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testShortRemove_whenArrayOutOfIndex() {
        instance.testShortRemove(new Short[] { 1, 2, 3 }, 9);
    }

    @Test(expected = OpenLRuntimeException.class)
    public void testShortRemove_whenSourceIsNull() {
        instance.testShortRemove(null, 1);
    }

    @Test
    public void testRemoveElementBooleanType() {
        boolean[] inputArray1 = { true, false, true };
        boolean[] outputArray = instance.testBooleanTypeRemoveElement(inputArray1, true);
        assertTrue(Arrays.equals(new boolean[] { false, true }, outputArray));

        outputArray = instance.testBooleanTypeRemoveElement(null, true);
        assertNull(outputArray);
        inputArray1 = new boolean[] { true, true, true };
        outputArray = instance.testBooleanTypeRemoveElement(inputArray1, false);
        assertTrue(Arrays.equals(new boolean[] { true, true, true }, outputArray));
        inputArray1 = new boolean[] {};
        outputArray = instance.testBooleanTypeRemoveElement(inputArray1, false);
        assertTrue(Arrays.equals(new boolean[] {}, outputArray));
    }

    @Test
    public void testRemoveElementBoolean() {
        Boolean[] inputArray1 = { true, false, true };
        Boolean[] outputArray = instance.testBooleanRemoveElement(inputArray1, true);
        assertTrue(Arrays.equals(new Boolean[] { false, true }, outputArray));

        outputArray = instance.testBooleanRemoveElement(null, true);
        assertNull(outputArray);
        inputArray1 = new Boolean[] { true, true, true };
        outputArray = instance.testBooleanRemoveElement(inputArray1, false);
        assertTrue(Arrays.equals(new Boolean[] { true, true, true }, outputArray));
        inputArray1 = new Boolean[] {};
        outputArray = instance.testBooleanRemoveElement(inputArray1, false);
        assertTrue(Arrays.equals(new Boolean[] {}, outputArray));
    }

    @Test
    public void testRemoveElementByteType() {
        byte[] inputArray1 = { 0, 7, 0 };
        byte[] outputArray = instance.testByteTypeRemoveElement(inputArray1, (byte) 0);
        assertTrue(Arrays.equals(new byte[] { 7, 0 }, outputArray));

        outputArray = instance.testByteTypeRemoveElement(null, (byte) 7);
        assertNull(outputArray);
        inputArray1 = new byte[] { 6, 7, 8 };
        outputArray = instance.testByteTypeRemoveElement(inputArray1, (byte) 9);
        assertTrue(Arrays.equals(new byte[] { 6, 7, 8 }, outputArray));
        inputArray1 = new byte[] {};
        outputArray = instance.testByteTypeRemoveElement(inputArray1, (byte) 5);
        assertTrue(Arrays.equals(new byte[] {}, outputArray));
    }

    @Test
    public void testRemoveElementByte() {
        Byte[] inputArray1 = { 0, 7, 0 };
        Byte[] outputArray = instance.testByteRemoveElement(inputArray1, (byte) 0);
        assertTrue(Arrays.equals(new Byte[] { 7, 0 }, outputArray));

        outputArray = instance.testByteRemoveElement(null, (byte) 7);
        assertNull(outputArray);
        inputArray1 = new Byte[] { 6, 7, 8 };
        outputArray = instance.testByteRemoveElement(inputArray1, (byte) 9);
        assertTrue(Arrays.equals(new Byte[] { 6, 7, 8 }, outputArray));
        inputArray1 = new Byte[] {};
        outputArray = instance.testByteRemoveElement(inputArray1, (byte) 5);
        assertTrue(Arrays.equals(new Byte[] {}, outputArray));
    }

    @Test
    public void testRemoveElementCharType() {
        char[] inputArray1 = { '0', '7', '0' };
        char[] outputArray = instance.testCharTypeRemoveElement(inputArray1, '0');
        assertTrue(Arrays.equals(new char[] { '7', '0' }, outputArray));

        outputArray = instance.testCharTypeRemoveElement(null, '7');
        assertNull(outputArray);
        inputArray1 = new char[] { '6', '7', '8' };
        outputArray = instance.testCharTypeRemoveElement(inputArray1, '9');
        assertTrue(Arrays.equals(new char[] { '6', '7', '8' }, outputArray));
        inputArray1 = new char[] {};
        outputArray = instance.testCharTypeRemoveElement(inputArray1, '5');
        assertTrue(Arrays.equals(new char[] {}, outputArray));
    }

    @Test
    public void testRemoveElementChar() {
        Character[] inputArray1 = { '0', '7', '0' };
        Character[] outputArray = instance.testCharRemoveElement(inputArray1, '0');
        assertTrue(Arrays.equals(new Character[] { '7', '0' }, outputArray));

        outputArray = instance.testCharRemoveElement(null, '7');
        assertNull(outputArray);
        inputArray1 = new Character[] { '6', '7', '8' };
        outputArray = instance.testCharRemoveElement(inputArray1, '9');
        assertTrue(Arrays.equals(new Character[] { '6', '7', '8' }, outputArray));
        inputArray1 = new Character[] {};
        outputArray = instance.testCharRemoveElement(inputArray1, '5');
        assertTrue(Arrays.equals(new Character[] {}, outputArray));
    }

    @Test
    public void testRemoveElementDoubleType() {
        double[] inputArray1 = { 0.1, 7.1, 0.1 };
        double[] outputArray = instance.testDoubleTypeRemoveElement(inputArray1, 0.1);
        assertTrue(Arrays.equals(new double[] { 7.1, 0.1 }, outputArray));

        outputArray = instance.testDoubleTypeRemoveElement(null, 7.1);
        assertNull(outputArray);
        inputArray1 = new double[] { 6.1, 7.1, 8.1 };
        outputArray = instance.testDoubleTypeRemoveElement(inputArray1, 9.1);
        assertTrue(Arrays.equals(new double[] { 6.1, 7.1, 8.1 }, outputArray));
        inputArray1 = new double[] {};
        outputArray = instance.testDoubleTypeRemoveElement(inputArray1, 5.1);
        assertTrue(Arrays.equals(new double[] {}, outputArray));
    }

    @Test
    public void testRemoveElementDouble() {
        Double[] inputArray1 = { 0.1, 7.1, 0.1 };
        Double[] outputArray = instance.testDoubleRemoveElement(inputArray1, 0.1);
        assertTrue(Arrays.equals(new Double[] { 7.1, 0.1 }, outputArray));

        outputArray = instance.testDoubleRemoveElement(null, 7.1);
        assertNull(outputArray);
        inputArray1 = new Double[] { 6.1, 7.1, 8.1 };
        outputArray = instance.testDoubleRemoveElement(inputArray1, 9.1);
        assertTrue(Arrays.equals(new Double[] { 6.1, 7.1, 8.1 }, outputArray));
        inputArray1 = new Double[] {};
        outputArray = instance.testDoubleRemoveElement(inputArray1, 5.1);
        assertTrue(Arrays.equals(new Double[] {}, outputArray));
    }

    @Test
    public void testRemoveElementFloatType() {
        float[] inputArray1 = { 0.1f, 7.1f, 0.1f };
        float[] outputArray = instance.testFloatTypeRemoveElement(inputArray1, 0.1f);
        assertTrue(Arrays.equals(new float[] { 7.1f, 0.1f }, outputArray));

        outputArray = instance.testFloatTypeRemoveElement(null, 7.1f);
        assertNull(outputArray);
        inputArray1 = new float[] { 6.1f, 7.1f, 8.1f };
        outputArray = instance.testFloatTypeRemoveElement(inputArray1, 9.1f);
        assertTrue(Arrays.equals(new float[] { 6.1f, 7.1f, 8.1f }, outputArray));
        inputArray1 = new float[] {};
        outputArray = instance.testFloatTypeRemoveElement(inputArray1, 5.1f);
        assertTrue(Arrays.equals(new float[] {}, outputArray));
    }

    @Test
    public void testRemoveElementFloat() {
        Float[] inputArray1 = { 0.1f, 7.1f, 0.1f };
        Float[] outputArray = instance.testFloatRemoveElement(inputArray1, 0.1f);
        assertTrue(Arrays.equals(new Float[] { 7.1f, 0.1f }, outputArray));

        outputArray = instance.testFloatRemoveElement(null, 7.1f);
        assertNull(outputArray);
        inputArray1 = new Float[] { 6.1f, 7.1f, 8.1f };
        outputArray = instance.testFloatRemoveElement(inputArray1, 9.1f);
        assertTrue(Arrays.equals(new Float[] { 6.1f, 7.1f, 8.1f }, outputArray));
        inputArray1 = new Float[] {};
        outputArray = instance.testFloatRemoveElement(inputArray1, 5.1f);
        assertTrue(Arrays.equals(new Float[] {}, outputArray));
    }

    @Test
    public void testRemoveElementIntegerType() {
        int[] inputArray1 = { 0, 7, 0 };
        int[] outputArray = instance.testIntegerTypeRemoveElement(inputArray1, 0);
        assertTrue(Arrays.equals(new int[] { 7, 0 }, outputArray));

        outputArray = instance.testIntegerTypeRemoveElement(null, 7);
        assertNull(outputArray);
        inputArray1 = new int[] { 6, 7, 8 };
        outputArray = instance.testIntegerTypeRemoveElement(inputArray1, 9);
        assertTrue(Arrays.equals(new int[] { 6, 7, 8 }, outputArray));
        inputArray1 = new int[] {};
        outputArray = instance.testIntegerTypeRemoveElement(inputArray1, 5);
        assertTrue(Arrays.equals(new int[] {}, outputArray));
    }

    @Test
    public void testRemoveElementInteger() {
        Integer[] inputArray1 = { 0, 7, 0 };
        Integer[] outputArray = instance.testIntegerRemoveElement(inputArray1, 0);
        assertTrue(Arrays.equals(new Integer[] { 7, 0 }, outputArray));

        outputArray = instance.testIntegerRemoveElement(null, 7);
        assertNull(outputArray);
        inputArray1 = new Integer[] { 6, 7, 8 };
        outputArray = instance.testIntegerRemoveElement(inputArray1, 9);
        assertTrue(Arrays.equals(new Integer[] { 6, 7, 8 }, outputArray));
        inputArray1 = new Integer[] {};
        outputArray = instance.testIntegerRemoveElement(inputArray1, 5);
        assertTrue(Arrays.equals(new Integer[] {}, outputArray));
    }

    @Test
    public void testRemoveElementLongType() {
        long[] inputArray1 = { 0, 7, 0 };
        long[] outputArray = instance.testLongTypeRemoveElement(inputArray1, 0);
        assertTrue(Arrays.equals(new long[] { 7, 0 }, outputArray));

        outputArray = instance.testLongTypeRemoveElement(null, 7);
        assertNull(outputArray);
        inputArray1 = new long[] { 6, 7, 8 };
        outputArray = instance.testLongTypeRemoveElement(inputArray1, 9);
        assertTrue(Arrays.equals(new long[] { 6, 7, 8 }, outputArray));
        inputArray1 = new long[] {};
        outputArray = instance.testLongTypeRemoveElement(inputArray1, 5);
        assertTrue(Arrays.equals(new long[] {}, outputArray));
    }

    @Test
    public void testRemoveElementLong() {
        Long[] inputArray1 = { 0L, 7L, 0L };
        Long[] outputArray = instance.testLongRemoveElement(inputArray1, 0L);
        assertTrue(Arrays.equals(new Long[] { 7L, 0L }, outputArray));

        outputArray = instance.testLongRemoveElement(null, 7L);
        assertNull(outputArray);
        inputArray1 = new Long[] { 6L, 7L, 8L };
        outputArray = instance.testLongRemoveElement(inputArray1, 9L);
        assertTrue(Arrays.equals(new Long[] { 6L, 7L, 8L }, outputArray));
        inputArray1 = new Long[] {};
        outputArray = instance.testLongRemoveElement(inputArray1, 5L);
        assertTrue(Arrays.equals(new Long[] {}, outputArray));
    }

    @Test
    public void testRemoveElementObject() {
        Object[] inputArray1 = { 0, 7, 0 };
        Object[] outputArray = instance.testObjectTypeRemoveElement(inputArray1, 0);
        assertTrue(Arrays.equals(new Object[] { 7, 0 }, outputArray));

        outputArray = instance.testObjectTypeRemoveElement(null, 7);
        assertNull(outputArray);
        inputArray1 = new Object[] { 6, 7, 8 };
        outputArray = instance.testObjectTypeRemoveElement(inputArray1, 9);
        assertTrue(Arrays.equals(new Object[] { 6, 7, 8 }, outputArray));
        inputArray1 = new Object[] {};
        outputArray = instance.testObjectTypeRemoveElement(inputArray1, 5);
        assertTrue(Arrays.equals(new Object[] {}, outputArray));
    }

    @Test
    public void testRemoveElementShortType() {
        short[] inputArray1 = { 0, 7, 0 };
        short[] outputArray = instance.testShortTypeRemoveElement(inputArray1, (short) 0);
        assertTrue(Arrays.equals(new short[] { 7, 0 }, outputArray));

        outputArray = instance.testShortTypeRemoveElement(null, (short) 7);
        assertNull(outputArray);
        inputArray1 = new short[] { 6, 7, 8 };
        outputArray = instance.testShortTypeRemoveElement(inputArray1, (short) 9);
        assertTrue(Arrays.equals(new short[] { 6, 7, 8 }, outputArray));
        inputArray1 = new short[] {};
        outputArray = instance.testShortTypeRemoveElement(inputArray1, (short) 5);
        assertTrue(Arrays.equals(new short[] {}, outputArray));
    }

    @Test
    public void testRemoveElementShort() {
        Short[] inputArray1 = { 0, 7, 0 };
        Short[] outputArray = instance.testShortRemoveElement(inputArray1, (short) 0);
        assertTrue(Arrays.equals(new Short[] { 7, 0 }, outputArray));

        outputArray = instance.testShortRemoveElement(null, (short) 7);
        assertNull(outputArray);
        inputArray1 = new Short[] { 6, 7, 8 };
        outputArray = instance.testShortRemoveElement(inputArray1, (short) 9);
        assertTrue(Arrays.equals(new Short[] { 6, 7, 8 }, outputArray));
        inputArray1 = new Short[] {};
        outputArray = instance.testShortRemoveElement(inputArray1, (short) 5);
        assertTrue(Arrays.equals(new Short[] {}, outputArray));
    }

    @Test
    public void testIsEmptyObject() {
        Object[] inputArray = { 1, 2, 3, 4 };
        assertFalse(instance.testObjectIsEmpty(inputArray));
        inputArray = new Object[] {};
        assertTrue(instance.testObjectIsEmpty(inputArray));
        assertTrue(instance.testObjectIsEmpty(null));
    }

    @Test
    public void testIsEmptyByteType() {
        byte[] inputArray = { 1, 2, 3, 4 };
        assertFalse(instance.testByteTypeIsEmpty(inputArray));
        inputArray = new byte[] {};
        assertTrue(instance.testByteTypeIsEmpty(inputArray));
        assertTrue(instance.testByteTypeIsEmpty(null));
    }

    @Test
    public void testIsEmptyCharType() {
        char[] inputArray = { '1', '2', '3', '4' };
        assertFalse(instance.testCharTypeIsEmpty(inputArray));
        inputArray = new char[] {};
        assertTrue(instance.testCharTypeIsEmpty(inputArray));
        assertTrue(instance.testCharTypeIsEmpty(null));
    }

    @Test
    public void testIsEmptyShortType() {
        short[] inputArray = { 1, 2, 3, 4 };
        assertFalse(instance.testShortTypeIsEmpty(inputArray));
        inputArray = new short[] {};
        assertTrue(instance.testShortTypeIsEmpty(inputArray));
        assertTrue(instance.testShortTypeIsEmpty(null));
    }

    @Test
    public void testIsEmptyIntegerType() {
        int[] inputArray = { 1, 2, 3, 4 };
        assertFalse(instance.testIntegerTypeIsEmpty(inputArray));
        inputArray = new int[] {};
        assertTrue(instance.testIntegerTypeIsEmpty(inputArray));
        assertTrue(instance.testIntegerTypeIsEmpty(null));
    }

    @Test
    public void testIsEmptyLongType() {
        long[] inputArray = { 1, 2, 3, 4 };
        assertFalse(instance.testLongTypeIsEmpty(inputArray));
        inputArray = new long[] {};
        assertTrue(instance.testLongTypeIsEmpty(inputArray));
        assertTrue(instance.testLongTypeIsEmpty(null));
    }

    @Test
    public void testIsEmptyFloatType() {
        float[] inputArray = { 1.1f, 2.1f, 3.1f, 4.1f };
        assertFalse(instance.testFloatTypeIsEmpty(inputArray));
        inputArray = new float[] {};
        assertTrue(instance.testFloatTypeIsEmpty(inputArray));
        assertTrue(instance.testFloatTypeIsEmpty(null));
    }

    @Test
    public void testIsEmptyDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2009, Calendar.DECEMBER, 12);
        Date[] inputArray = { cal.getTime() };
        assertFalse(instance.testDateIsEmpty(inputArray));
        inputArray = new Date[] {};
        assertTrue(instance.testDateIsEmpty(inputArray));
        assertTrue(instance.testDateIsEmpty(null));
    }

    @Test
    public void testIsEmptyBigDecimalType() {
        BigDecimal[] inputArray = { BigDecimal.valueOf(1),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(4) };
        assertFalse(instance.testBigDecimalIsEmpty(inputArray));
        inputArray = new BigDecimal[] {};
        assertTrue(instance.testBigDecimalIsEmpty(inputArray));
        assertTrue(instance.testBigDecimalIsEmpty(null));
    }

    @Test
    public void testIsEmptyBigIntegerType() {
        BigInteger[] inputArray = { BigInteger.valueOf(1),
                BigInteger.valueOf(2),
                BigInteger.valueOf(3),
                BigInteger.valueOf(4) };
        assertFalse(instance.testBigIntegerIsEmpty(inputArray));
        inputArray = new BigInteger[] {};
        assertTrue(instance.testBigIntegerIsEmpty(inputArray));
        assertTrue(instance.testBigIntegerIsEmpty(null));
    }

    @Test
    public void testIsEmptyStringType() {
        String[] inputArray = { "1", "2", "3", "4" };
        assertFalse(instance.testStringIsEmpty(inputArray));
        inputArray = new String[] {};
        assertTrue(instance.testStringIsEmpty(inputArray));
        assertTrue(instance.testStringIsEmpty(null));
    }

    @Test
    public void testOrCallingFromRules() {
        assertTrue(instance.checkOr());
    }

    @Test
    public void testStartsWith() {
        String prefix = "Test";
        assertTrue(instance.testStartsWith(str, prefix));
        assertFalse(instance.testStartsWith(null, prefix));
    }

    @Test
    public void testEndWith() {
        String prefix = "value";
        assertTrue(instance.testEndsWith(str, prefix));
        assertFalse(instance.testEndsWith(null, prefix));
    }

    @Test
    public void testSubString() {
        int beginIndex = 3;
        int endIndex = 5;

        assertEquals("ting string value", instance.testSubString(str, beginIndex));
        assertEquals("ti", instance.testSubString(str, beginIndex, endIndex));
        assertEquals("", instance.testSubString("", beginIndex));
        assertNull(instance.testSubString(null, 0));

    }

    @Test
    public void testRemoveStart() {
        String remove = "Testing";

        assertEquals(" string value", instance.testRemoveStart(str, remove));
        assertNull(instance.testRemoveStart(null, remove));
        assertEquals("", instance.testRemoveStart("", remove));
    }

    @Test
    public void testRemoveEnd() {
        String remove = "value";

        assertEquals("Testing string ", instance.testRemoveEnd(str, remove));
        assertNull(instance.testRemoveEnd(null, remove));
        assertEquals("", instance.testRemoveEnd("", remove));
    }

    @Test
    public void testStringCase() {
        String str = "Testing";

        assertEquals("TESTING", instance.testUpperCase(str));
        assertEquals("testing", instance.testLowerCase(str));
        assertNull(instance.testUpperCase(null));
        assertNull(instance.testLowerCase(null));
        assertEquals("", instance.testUpperCase(""));
        assertEquals("", instance.testLowerCase(""));
    }

    @Test
    public void testReplace() {
        String text = "value Teting value string value";

        assertEquals("Testing string text", instance.testReplace(str, "value", "text"));
        assertEquals("text Teting text string text", instance.testReplace(text, "value", "text", 3));
        assertNull(instance.testReplace(null, "value", "text"));
        assertEquals("", instance.testReplace("", "value", "text"));
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

        c.set(year, month, date, hour, min);

        assertEquals("2/25/13", RulesUtils.format(c.getTime()));
        assertEquals("2/25/13", RulesUtils.dateToString(c.getTime()));

        assertEquals("25/13", RulesUtils.format(c.getTime(), "dd/yy"));
        assertEquals("25/13", RulesUtils.dateToString(c.getTime(), "dd/yy"));

        assertEquals("25/13 15:03", RulesUtils.format(c.getTime(), "dd/yy HH:mm"));
        assertEquals("25/13 15:03", RulesUtils.dateToString(c.getTime(), "dd/yy HH:mm"));

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testStringToDate_shouldThrowException() throws ParseException {
        Calendar c = Calendar.getInstance();

        c.set(2015, 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        assertEquals(c.getTime(), RulesUtils.stringToDate("2/1/2015"));
    }

    @Test
    public void quotientIntTest() {
        assertEquals(2, RulesUtils.quotient(9, 4));
    }

    @Test
    public void flattenTest() {
        Object[] res = instance.testFlatten(1, 2, 3, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, res);
        assertEquals(Integer.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Integer[] { 1, 2, 3, 4 });
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, res);
        assertEquals(Integer.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Integer[] { 1, 2 }, 3, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, res);
        assertEquals(Integer.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Integer[] { 1, 2 }, new Integer[] { 3 }, new Integer[] {}, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, res);
        assertEquals(Integer.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Integer[][] { { 1 }, { 2 }, { 3, 4 } });
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, res);
        assertEquals(Integer.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Object[] { 1, new Object[] { 2 }, 3 }, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, res);
        assertEquals(Integer.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Object[] { "1", new Integer[] { 2 }, 3L }, 4D);
        assertArrayEquals(new Object[] { "1", 2, 3L, 4D }, res);
        assertEquals(Object.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Object[] { 1, new Integer[] { 2 }, 3L }, 4D);
        assertArrayEquals(new Object[] { 1, 2, 3L, 4D }, res);
        assertEquals(Number.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Object[] { null, new Object[] { null }, 3 }, null);
        assertArrayEquals(new Integer[] { null, null, 3, null }, res);
        assertEquals(Integer.class, res.getClass().getComponentType());

        res = instance.testFlatten(new Object[] { null, new Object[] { null }, null }, null);
        assertArrayEquals(new Integer[] { null, null, null, null }, res);
        assertEquals(Void.class, res.getClass().getComponentType());
    }

    @Test
    public void getValuesTest() {
        Object[] agency = instance.testGetValuesAlias();
        assertArrayEquals(new String[] { "Alias1", "Alias2", "Alias0", "Alias4" }, agency);
        assertEquals(String.class, agency.getClass().getComponentType());

        Object[] primes = instance.testGetValuesPrimesAlias();
        assertArrayEquals(new Integer[] { 5, 7, 13, 17 }, primes);
        assertEquals(Integer.class, primes.getClass().getComponentType());
    }

    @Test
    public void testMaxDoubleArray() {
        assertEquals(new Double(10.44), instance.testMaxDouble(new Double[] { -1.4, 5.77, 7., 10.44, -10.44 }));
        assertNull(instance.testMaxDouble(null));
        assertNull(instance.testMaxDouble(new Double[] {}));
    }

    @Test
    public void testMaxDoubleTypeArray() {
        assertEquals(new Double(10.44), instance.testMaxDoubleType(new double[] { -1.4, 5.77, 7., 10.44, -10.44 }));
        assertNull(instance.testMaxDoubleType(null));
        assertNull(instance.testMaxDoubleType(new double[] {}));
    }

    @Test
    public void testMinDoubleArray() {
        assertEquals(new Double(-10.44), instance.testMinDouble(new Double[] { -1.4, 5.77, 7., 10.44, -10.44 }));
        assertNull(instance.testMinDouble(null));
        assertNull(instance.testMinDouble(new Double[] {}));
    }

    @Test
    public void testMinDoubleTypeArray() {
        assertEquals(new Double(-10.44), instance.testMinDoubleType(new double[] { -1.4, 5.77, 7., 10.44, -10.44 }));
        assertNull(instance.testMinDoubleType(null));
        assertNull(instance.testMinDoubleType(new double[] {}));
    }

    @Test
    public void testSumDoubleArray() {
        assertEquals(new Double(11.37), instance.testSumDouble(new Double[] { -1.4, 5.77, 7., 10.44, -10.44 }));
        assertNull(instance.testSumDouble(null));
        // TODO: is it correct behavior for empty array?
        assertNull(instance.testSumDouble(new Double[] {}));
    }

    @Test
    public void testSumDoubleTypeArray() {
        assertEquals(new Double(11.37), instance.testSumDoubleType(new double[] { -1.4, 5.77, 7., 10.44, -10.44 }));
        assertNull(instance.testSumDoubleType(null));
        assertEquals(new Double(0), instance.testSumDoubleType(new double[] {}));
    }

    @Test
    public void testAvgBigDecimalArray() {
        assertEquals(new BigDecimal("2.28"),
            instance.testAvgBigDecimalArray(new BigDecimal[] { new BigDecimal("-1.4"),
                    new BigDecimal("5.77"),
                    new BigDecimal("7"),
                    new BigDecimal("10.47"),
                    new BigDecimal("-10.44") }));
        assertNull(instance.testAvgBigDecimalArray(null));
        assertNull(instance.testAvgBigDecimalArray(new BigDecimal[] {}));
    }

    @Test
    public void testAvgBigIntegerArray() {
        assertEquals(new BigDecimal(5.25),
            instance.testAvgBigIntegerArray(new BigInteger[] { BigInteger.valueOf(-1),
                    BigInteger.valueOf(5),
                    BigInteger.valueOf(7),
                    BigInteger.valueOf(10) }));
        assertNull(instance.testAvgBigIntegerArray(null));
        assertNull(instance.testAvgBigIntegerArray(new BigInteger[] {}));
    }

    @Test
    public void testMedianByte() {
        assertEquals(new Double(5), instance.testMedianByte(new Byte[] { 11, 9, 3, 5, 5 }));
        assertEquals(new Double(3.5), instance.testMedianByte(new Byte[] { 2, 1, 5, 7 }));
        assertNull(instance.testMedianByte(null));
        assertNull(instance.testMedianByte(new Byte[] {}));
    }

    @Test
    public void testMedianByteType() {
        assertEquals(new Double(5), instance.testMedianByteType(new byte[] { 11, 9, 3, 5, 5 }));
        assertEquals(new Double(3.5), instance.testMedianByteType(new byte[] { 2, 1, 5, 7 }));
        assertNull(instance.testMedianByteType(null));
        // TODO: Is it correct? The result is different when used boxed array
        assertEquals(null, instance.testMedianByteType(new byte[] {}));
    }

    @Test
    public void testMedianShort() {
        assertEquals(new Double(5), instance.testMedianShort(new Short[] { 11, 9, 3, 5, 5 }));
        assertEquals(new Double(3.5), instance.testMedianShort(new Short[] { 2, 1, 5, 7 }));
        assertNull(instance.testMedianShort(null));
        assertNull(instance.testMedianShort(new Short[] {}));
    }

    @Test
    public void testMedianShortType() {
        assertEquals(new Double(5), instance.testMedianShortType(new short[] { 11, 9, 3, 5, 5 }));
        assertEquals(new Double(3.5), instance.testMedianShortType(new short[] { 2, 1, 5, 7 }));
        assertNull(instance.testMedianShortType(null));
        assertEquals(null, instance.testMedianShortType(new short[] {}));
    }

    @Test
    public void testMedianInteger() {
        assertEquals(new Double(5), instance.testMedianInteger(new Integer[] { 11, 9, 3, 5, 5 }));
        assertEquals(new Double(3.5), instance.testMedianInteger(new Integer[] { 2, 1, 5, 7 }));
        assertNull(instance.testMedianInteger(null));
        assertNull(instance.testMedianInteger(new Integer[] {}));
    }

    @Test
    public void testMedianIntegerType() {
        assertEquals(new Double(5), instance.testMedianIntegerType(new int[] { 11, 9, 3, 5, 5 }));
        assertEquals(new Double(3.5), instance.testMedianIntegerType(new int[] { 2, 1, 5, 7 }));
        assertNull(instance.testMedianIntegerType(null));
        assertEquals(null, instance.testMedianIntegerType(new int[] {}));
    }

    @Test
    public void testMedianLong() {
        assertEquals(new Double(5), instance.testMedianLong(new Long[] { 11L, 9L, 3L, 5L, 5L }));
        assertEquals(new Double(3.5), instance.testMedianLong(new Long[] { 2L, 1L, 5L, 7L }));
        assertNull(instance.testMedianLong(null));
        assertNull(instance.testMedianLong(new Long[] {}));
    }

    @Test
    public void testMedianLongType() {
        assertEquals(new Double(5), instance.testMedianLongType(new long[] { 11L, 9L, 3L, 5L, 5L }));
        assertEquals(new Double(3.5), instance.testMedianLongType(new long[] { 2L, 1L, 5L, 7L }));
        assertNull(instance.testMedianLongType(null));
        assertEquals(null, instance.testMedianLongType(new long[] {}));
    }

    @Test
    public void testMedianFloat() {
        assertEquals(new Float(5), instance.testMedianFloat(new Float[] { 11.F, 9.F, 3.F, 5.F, 5.F }));
        assertEquals(new Float(3.5), instance.testMedianFloat(new Float[] { 2.F, 1.F, 5.F, 7.F }));
        assertNull(instance.testMedianFloat(null));
        assertNull(instance.testMedianFloat(new Float[] {}));
    }

    @Test
    public void testMedianFloatType() {
        assertEquals(new Float(5), instance.testMedianFloatType(new float[] { 11.F, 9.F, 3.F, 5.F, 5.F }));
        assertEquals(new Float(3.5), instance.testMedianFloatType(new float[] { 2.F, 1.F, 5.F, 7.F }));
        assertNull(instance.testMedianFloatType(null));
        assertEquals(null, instance.testMedianFloatType(new float[] {}));
    }

    @Test
    public void testMedianDouble() {
        assertEquals(new Double(5), instance.testMedianDouble(new Double[] { 11., 9., 3., 5., 5. }));
        assertEquals(new Double(3.5), instance.testMedianDouble(new Double[] { 2., 1., 5., 7. }));
        assertNull(instance.testMedianDouble(null));
        assertNull(instance.testMedianDouble(new Double[] {}));
    }

    @Test
    public void testMedianDoubleType() {
        assertEquals(new Double(5), instance.testMedianDoubleType(new double[] { 11., 9., 3., 5., 5. }));
        assertEquals(new Double(3.5), instance.testMedianDoubleType(new double[] { 2., 1., 5., 7. }));
        assertNull(instance.testMedianDoubleType(null));
        assertEquals(null, instance.testMedianDoubleType(new double[] {}));
    }

    @Test
    public void testMedianBigDecimal() {
        assertEquals(BigDecimal.valueOf(5),
            instance.testMedianBigDecimal(new BigDecimal[] { BigDecimal.valueOf(
                11), BigDecimal.valueOf(9), BigDecimal.valueOf(3), BigDecimal.valueOf(5), BigDecimal.valueOf(5) }));
        assertEquals(BigDecimal.valueOf(3.5),
            instance.testMedianBigDecimal(new BigDecimal[] { BigDecimal.valueOf(2),
                    BigDecimal.valueOf(1),
                    BigDecimal.valueOf(5),
                    BigDecimal.valueOf(7) }));
        assertNull(instance.testMedianBigDecimal(null));
        assertNull(instance.testMedianBigDecimal(new BigDecimal[] {}));
    }

    @Test
    public void testMedianBigInteger() {
        assertEquals(BigDecimal.valueOf(5),
            instance.testMedianBigInteger(new BigInteger[] { BigInteger.valueOf(
                11), BigInteger.valueOf(9), BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(5) }));
        assertEquals(BigDecimal.valueOf(3.5),
            instance.testMedianBigInteger(new BigInteger[] { BigInteger.valueOf(2),
                    BigInteger.valueOf(1),
                    BigInteger.valueOf(5),
                    BigInteger.valueOf(7) }));
        assertNull(instance.testMedianBigInteger(null));
        assertNull(instance.testMedianBigInteger(new BigInteger[] {}));
    }

    @Test
    public void testModDouble() {
        assertEquals(new Double(0.5), instance.testModDouble(new Double(10.1), new Double(3.2)), 1e-7);
        assertEquals(new Double(0), instance.testModDouble(new Double(10.1), null));
        assertEquals(new Double(0), instance.testModDouble(null, new Double(3.2)));
    }

    @Test
    public void testModDoubleType() {
        assertEquals(0.5, instance.testModDoubleType(10.1, 3.2), 1e-7);
    }

    @SuppressWarnings("deprecated")
    @Test
    public void testParseFormattedDouble() throws ParseException {
        assertEquals(1.1f, RulesUtils.parseFormattedDouble("1.1"), 0.00001);
        assertEquals(1441.33f, RulesUtils.parseFormattedDouble("1,441.33", "#,##0.00"), 0.01);
    }


    @Test
    public void testCopy() {
        assertEquals(RulesUtils.copy("1.1"), "1.1");
        assertEquals(RulesUtils.copy(1.1), Double.valueOf(1.1));

        Some s = new Some();
        s.str = "str1";
        s.num = 10;
        s.num2 = 12;
        s.obj = s;
        Some copy = RulesUtils.copy(s);
        assertNotSame(copy, s);
        assertSame(s.obj, s);
        assertSame(copy.obj, copy);
        assertEquals(copy.str, s.str);
        assertEquals(copy.num, s.num);
        assertEquals(copy.num2, s.num2);

        copy.num = 4;

        assertEquals(copy.num, 4);
        assertEquals(copy.obj.num, 4);
        assertEquals(s.num, 10);
        assertEquals(s.obj.num, 10);
    }

    private static class Some {
        String str;
        int num;
        Integer num2;
        Some obj;
    }
}
