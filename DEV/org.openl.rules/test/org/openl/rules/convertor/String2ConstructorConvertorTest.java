package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.time.LocalDate;

import org.junit.Test;

public class String2ConstructorConvertorTest {

    @Test
    public void testParseConstructor() {
        String2ConstructorConvertor<Integer> converter = new String2ConstructorConvertor<>(Integer.class);
        Integer result = converter.parse("123", null);
        assertEquals((Integer) 123, result);
    }

    @Test
    public void testParseValueOf() {
        String2ConstructorConvertor<Timestamp> converter = new String2ConstructorConvertor<>(Timestamp.class);
        Timestamp result = converter.parse("1980-07-12 00:00:00", null);
        assertEquals(Timestamp.valueOf("1980-07-12 00:00:00"), result);
    }

    @Test
    public void testParseParse() {
        String2ConstructorConvertor<LocalDate> converter = new String2ConstructorConvertor<>(LocalDate.class);
        LocalDate result = converter.parse("1980-07-12", null);
        assertEquals(LocalDate.of(1980, 7, 12), result);
    }

    @Test
    public void testParseNull() {
        String2ConstructorConvertor<?> converter = new String2ConstructorConvertor<>(Integer.class);
        assertNull(converter.parse(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNoConstructor() {
        new String2ConstructorConvertor<>(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatNoConstructor() {
        new String2ConstructorConvertor<>(Object.class);
    }
}
