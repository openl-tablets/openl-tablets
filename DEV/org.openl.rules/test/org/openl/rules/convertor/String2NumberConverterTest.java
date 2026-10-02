package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
                arguments("NaN", Double.NaN),
                arguments("Infinity", Double.POSITIVE_INFINITY),
                arguments("-Infinity", Double.NEGATIVE_INFINITY));
    }

    @ParameterizedTest(name = "\"{0}\" is parsed as {1}")
    @MethodSource
    void testParse(String text, Number expected) {
        String2NumberConverter<Number> converter = getNumberConverter();
        var result = converter.parse(text, null);
        assertEquals(expected, result);
    }

    @Test
    void testParseWithFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        var result = converter.parse("-3.1415$", "#,###$");
        assertEquals(-3.1415d, result);
    }

    @Test
    void testParseNull() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertNull(converter.parse(null, null));
    }

    // Not a number, empty, a percent sign alone, a lower case exponent and a trailing space
    @ParameterizedTest
    @ValueSource(strings = {"3.1415d", "", "%", "1e1", "1 "})
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
                arguments(BigInteger.class, "12345678901234567890001%"));
    }

    @ParameterizedTest(name = "\"{1}\" is not parsed")
    @MethodSource
    void testParseWholePercentWithFraction(Class<?> type, String text) {
        var e = assertThrows(NumberFormatException.class, () -> String2DataConvertorFactory.parse(type, text, null));
        assertEquals("Cannot convert '%s' to a number.".formatted(text), e.getMessage());
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
