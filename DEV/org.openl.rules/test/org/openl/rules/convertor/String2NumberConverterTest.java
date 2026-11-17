package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class String2NumberConverterTest {

    private Locale defaultLocale;

    @BeforeEach
    void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    static Stream<Arguments> testParse() {
        return Stream.of(
                arguments("3.1415", 3.1415d),
                arguments("-2.1415", -2.1415d),
                arguments("-.123456789", -.123456789d),
                arguments("0001.111000", 1.111d),
                arguments("-1.99999999999999934", -1.99999999999999934d),
                arguments("9223372036854775807", Long.MAX_VALUE),
                arguments("-9223372036854775808", Long.MIN_VALUE),
                arguments("9223372036854775808", 9223372036854775808d),
                arguments("-9223372036854775809", -9223372036854775809d),
                arguments("17.5%", 0.175d),
                arguments("1.234E2", 123.4d),
                arguments("-1.23E-3", -0.00123d),
                arguments("-1.23E4", -12300L),
                arguments("1.234e2", 123.4d),
                arguments("-1.23e-3", -0.00123d),
                arguments("-1.23e4", -12300L),
                arguments("1.5e1%", 0.15d),
                arguments("NaN", Double.NaN),
                arguments("Infinity", Double.POSITIVE_INFINITY),
                arguments("-Infinity", Double.NEGATIVE_INFINITY),
                arguments("1,000.00", 1000L),
                arguments("12,345,678", 12345678L),
                arguments("-1,234.5", -1234.5d),
                arguments("999,999.999", 999999.999d),
                arguments("1,000%", 10L),
                arguments("1,234.5e2", 123450L));
    }

    @ParameterizedTest(name = "\"{0}\" is parsed as {1}")
    @MethodSource
    void testParse(String text, Number expected) {
        String2NumberConverter<Number> converter = getNumberConverter();
        var result = converter.parse(text, null);
        assertEquals(expected, result);
    }

    // A comma separates groups of exactly three digits of the whole part, and the first group does not start with 0
    @ParameterizedTest
    @ValueSource(strings = {"12,34.00", "1,00,000", "100.000,001", "1,0000", "1234,567", "0,500", ",100", "1,000,",
            "1,000.000,001", "1,000 000", "-,100"})
    void testParseWrongThousandsSeparators(String text) {
        String2NumberConverter<Number> converter = getNumberConverter();
        var e = assertThrows(NumberFormatException.class, () -> converter.parse(text, null));
        assertEquals("Cannot convert '%s' to a number.".formatted(text), e.getMessage());
    }

    static Stream<Arguments> testParseThousandsOfEveryType() {
        return Stream.of(
                arguments(Short.class, "12,345", (short) 12345),
                arguments(Integer.class, "12,345,678", 12345678),
                arguments(Long.class, "-12,345,678,901", -12345678901L),
                arguments(Float.class, "1,000.5", 1000.5f),
                arguments(Double.class, "1,000.00", 1000d),
                arguments(Double.class, "-1,000.00", -1000d),
                arguments(BigInteger.class, "123,456,789,012,345,678,901", new BigInteger("123456789012345678901")),
                arguments(BigDecimal.class, "1,000.25", new BigDecimal("1000.25")));
    }

    @ParameterizedTest(name = "{0} reads \"{1}\" as {2}")
    @MethodSource
    void testParseThousandsOfEveryType(Class<?> type, String text, Number expected) {
        assertEquals(expected, String2DataConvertorFactory.parse(type, text, null));
    }

    // K, M and B multiply a number by a thousand, a million and a billion exactly
    static Stream<Arguments> testParseMultiplierOfEveryType() {
        return Stream.of(
                arguments(Byte.class, "0K", (byte) 0),
                arguments(Short.class, "32K", (short) 32000),
                arguments(Integer.class, "1.5K", 1500),
                arguments(Integer.class, "-2M", -2000000),
                arguments(Integer.class, "1,500K", 1500000),
                arguments(Long.class, "7B", 7000000000L),
                arguments(Long.class, "1.234567B", 1234567000L),
                arguments(Long.class, "2e3K", 2000000L),
                arguments(Float.class, "2.5K", 2500f),
                arguments(Double.class, "3323K", 3323000d),
                arguments(Double.class, "0.1K", 100d),
                arguments(Double.class, "1.0005K", 1000.5d),
                arguments(BigInteger.class, "123456789B", new BigInteger("123456789000000000")),
                arguments(BigDecimal.class, "1.5M", new BigDecimal("1.5E+6")));
    }

    @ParameterizedTest(name = "{0} reads \"{1}\" as {2}")
    @MethodSource
    void testParseMultiplierOfEveryType(Class<?> type, String text, Number expected) {
        assertEquals(expected, String2DataConvertorFactory.parse(type, text, null));
    }

    // A whole number type takes no fraction; a small letter, a space, a letter alone or with a percent is no multiplier
    static Stream<Arguments> testParseWrongMultiplier() {
        return Stream.of(
                arguments(Integer.class, "1.0005K"),
                arguments(BigInteger.class, "0.0005K"),
                arguments(Double.class, "2k"),
                arguments(Double.class, "2 K"),
                arguments(Double.class, "K"),
                arguments(Double.class, "2KK"),
                arguments(Double.class, "2K%"),
                arguments(Double.class, "NaNK"));
    }

    @ParameterizedTest(name = "{0} does not read \"{1}\"")
    @MethodSource
    void testParseWrongMultiplier(Class<?> type, String text) {
        assertNotANumber(type, text);
    }

    @Test
    void testParseMultiplierOutOfRange() {
        assertThrows(NumberFormatException.class, () -> String2DataConvertorFactory.parse(Integer.class, "3B", null));
    }

    @Test
    void testParseMultiplierWithFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertThrows(NumberFormatException.class, () -> converter.parse("2K", "#,###"));
    }

    @Test
    void testParseWithFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        var result = converter.parse("-3.1415$", "#,###$");
        assertEquals(-3.1415d, result);
    }

    // A small letter in a prefix or a suffix of the format is not an exponent
    @Test
    void testParseWithSmallLetterInFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertEquals(5L, converter.parse("5 each", "#' each'"));
        assertEquals(1L, converter.parse("e 1", "'e' 0"));
    }

    @Test
    void testParseNull() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertNull(converter.parse(null, null));
    }

    // Not a number, empty, a percent sign alone, an exponent letter without digits and a trailing space
    @ParameterizedTest
    @ValueSource(strings = {"3.1415d", "", "%", "1e", "e1", "1 "})
    void testParseInvalid(String text) {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertThrows(NumberFormatException.class, () -> converter.parse(text, null));
    }

    // A percent value without a fraction is a whole number
    static Stream<Arguments> testParseWholePercent() {
        return Stream.of(
                arguments(Byte.class, "-300%", (byte) -3),
                arguments(Short.class, "100%", (short) 1),
                arguments(Integer.class, "0%", 0),
                arguments(Long.class, "1200%", 12L),
                arguments(BigInteger.class, "12345678901234567890000%", new BigInteger("123456789012345678900")));
    }

    @ParameterizedTest(name = "\"{1}\" is parsed as {2}")
    @MethodSource
    void testParseWholePercent(Class<?> type, String text, Number expected) {
        assertEquals(expected, String2DataConvertorFactory.parse(type, text, null));
    }

    // A percent value or an exponent that leaves a fraction is not a whole number
    static Stream<Arguments> testParseWholePercentWithFraction() {
        return Stream.of(
                arguments(Byte.class, "-150%"),
                arguments(Short.class, "250%"),
                arguments(Integer.class, "5%"),
                arguments(Long.class, "250%"),
                arguments(Long.class, "25E-1"),
                arguments(Long.class, "25e-1"),
                arguments(BigInteger.class, "12345678901234567890001%"));
    }

    @ParameterizedTest(name = "\"{1}\" is not parsed")
    @MethodSource
    void testParseWholePercentWithFraction(Class<?> type, String text) {
        assertNotANumber(type, text);
    }

    private static void assertNotANumber(Class<?> type, String text) {
        var e = assertThrows(NumberFormatException.class, () -> String2DataConvertorFactory.parse(type, text, null));
        assertEquals("Cannot convert '%s' to a number.".formatted(text), e.getMessage());
    }

    static Stream<Arguments> testParseExponentOfEveryType() {
        return Stream.of(
                arguments(Byte.class, (byte) 100),
                arguments(Short.class, (short) 100),
                arguments(Integer.class, 100),
                arguments(Long.class, 100L),
                arguments(Float.class, 100f),
                arguments(Double.class, 100d),
                arguments(BigInteger.class, BigInteger.valueOf(100)),
                arguments(BigDecimal.class, new BigDecimal("1E+2")));
    }

    @ParameterizedTest(name = "{0} reads \"1E2\" and \"1e2\" as {1}")
    @MethodSource
    void testParseExponentOfEveryType(Class<?> type, Number expected) {
        var converter = String2DataConvertorFactory.getConvertor(type);
        assertEquals(expected, converter.parse("1E2", null));
        assertEquals(expected, converter.parse("1e2", null));
    }

    private String2NumberConverter<Number> getNumberConverter() {
        return new String2NumberConverter<Number>() {
            @Override
            Number convert(Number number, String data) {
                return number;
            }
        };
    }
}
