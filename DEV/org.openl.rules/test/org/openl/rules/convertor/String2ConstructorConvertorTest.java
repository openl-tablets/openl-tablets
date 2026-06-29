package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class String2ConstructorConvertorTest {

    @Test
    void testParseConstructor() {
        String2ConstructorConvertor<Integer> converter = new String2ConstructorConvertor<>(Integer.class);
        Integer result = converter.parse("123", null);
        assertEquals((Integer) 123, result);
    }

    @Test
    void testParseValueOf() {
        String2ConstructorConvertor<Timestamp> converter = new String2ConstructorConvertor<>(Timestamp.class);
        Timestamp result = converter.parse("1980-07-12 00:00:00", null);
        assertEquals(Timestamp.valueOf("1980-07-12 00:00:00"), result);
    }

    @Test
    void testParseParse() {
        String2ConstructorConvertor<LocalDate> converter = new String2ConstructorConvertor<>(LocalDate.class);
        LocalDate result = converter.parse("1980-07-12", null);
        assertEquals(LocalDate.of(1980, 7, 12), result);
    }

    @Test
    void testParseNull() {
        String2ConstructorConvertor<?> converter = new String2ConstructorConvertor<>(Integer.class);
        assertNull(converter.parse(null, null));
    }

    @Test
    void testParseNoConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new String2ConstructorConvertor<>(Object.class);
        });
    }

    @Test
    void testFormatNoConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new String2ConstructorConvertor<>(Object.class);
        });
    }
}
