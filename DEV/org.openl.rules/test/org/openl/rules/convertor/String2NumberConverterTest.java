package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    @Test
    void testParseNotNumber() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertThrows(NumberFormatException.class, () -> converter.parse("3.1415d", null));
    }

    @Test
    void testParseEmpty() {
        String2NumberConverter<Number> converter = getNumberConverter();
        // skip using a String Pool in runtime
        assertThrows(NumberFormatException.class, () -> converter.parse("", null));
    }

    @Test
    void testParsePercentSign() {
        String2NumberConverter<Number> converter = getNumberConverter();
        // skip using a String Pool in runtime
        assertThrows(NumberFormatException.class, () -> converter.parse("%", null));
    }

    @Test
    void testParseNotENumber() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertThrows(NumberFormatException.class, () -> converter.parse("1e1", null));
    }

    @Test
    void testParseWithSpaces() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertThrows(NumberFormatException.class, () -> converter.parse("1 ", null));
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
