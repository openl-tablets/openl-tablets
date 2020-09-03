package org.openl.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DateRangeDomainTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeClass
    public static void setUp() {
        Locale.setDefault(Locale.US);
        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);
    }

    @AfterClass
    public static void tearDown() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    @Test
    public void test() {
        final Date min = createDate(2020, 1, 1);
        final Date max = createDate(2020, 3, 31);

        final DateRangeDomain dateRange = new DateRangeDomain(min, max);
        assertEquals(min, dateRange.getMin());
        assertEquals(max, dateRange.getMax());
        assertEquals(createDate(2020, 2, 1), dateRange.getValue(31));
        assertEquals(31, dateRange.getIndex(createDate(2020, 2, 1)));
        assertEquals(90, dateRange.getIndex(max));
        assertEquals(-1, dateRange.getIndex(createDate(2019, 12, 30)));
        assertEquals(-1, dateRange.getIndex(createDate(2020, 4, 1)));
        assertTrue(dateRange.selectObject(createDate(2020, 2, 1)));
        assertTrue(dateRange.selectObject(createDate(2020, 1, 2)));
        assertTrue(dateRange.selectObject(createDate(2020, 3, 30)));
        assertFalse(dateRange.selectObject(createDate(2019, 12, 31)));
        assertFalse(dateRange.selectObject(createDate(2020, 4, 1)));
        assertNull(dateRange.getElementType());
        assertNull(dateRange.getValue(100));

        final Iterator<Date> it = dateRange.iterator();
        final LocalDateTime dateTime = min.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        for (int i = 0; i < 91; i++) {
            assertTrue(it.hasNext());
            it.remove(); //did nothing
            final Date expected = Date.from(dateTime.plusDays(i).atZone(ZoneId.systemDefault()).toInstant());
            final Date actual = it.next();
            assertEquals(expected, actual);
        }
        assertFalse(it.hasNext());
    }

    private static Date createDate(int year, int month, int dayOfMonth) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
