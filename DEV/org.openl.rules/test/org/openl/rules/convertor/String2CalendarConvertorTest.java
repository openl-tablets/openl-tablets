package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class String2CalendarConvertorTest {

    private Locale defaultLocale;

    @BeforeEach
    void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    void testParse() {
        Calendar time = Calendar.getInstance();
        time.setTime(new Date(114, 5, 17));
        String2CalendarConvertor converter = new String2CalendarConvertor();
        Calendar result = converter.parse("06/17/2014", null);
        assertEquals(time, result);
    }

    @Test
    void testParseByPattern() {
        Calendar time = Calendar.getInstance();
        time.setTime(new Date(114, 5, 17));
        String2CalendarConvertor converter = new String2CalendarConvertor();
        Calendar result = converter.parse("17-06-2014", "dd-MM-yyyy");
        assertEquals(time, result);
    }

    @Test
    void testParseEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2CalendarConvertor converter = new String2CalendarConvertor();
            converter.parse("", null);
        });
    }

    @Test
    void testParseWrongValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2CalendarConvertor converter = new String2CalendarConvertor();
            converter.parse("Kin-Dza-Dza", null);
        });
    }

    @Test
    void testParseNull() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        assertNull(converter.parse(null, null));
    }

}
