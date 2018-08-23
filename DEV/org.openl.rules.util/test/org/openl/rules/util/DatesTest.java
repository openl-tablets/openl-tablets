package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;

public class DatesTest {

    @Test
    public void testDate() {
        assertNull(Dates.toString(null));
        assertEquals(new Date(-1899, 0, 1), Dates.Date(1,1,1));
        assertEquals(new Date(118, 6, 12), Dates.Date(2018,7,12));
        assertEquals(new Date(80, 6, 12), Dates.Date(1980,7,12));
        assertEquals(new Date(-1820, 6, 12), Dates.Date(80,7,12));
        assertEquals(new Date(116, 1, 29), Dates.Date(2016,2,29));
    }

    @Test(expected = Exception.class)
    public void testDateWrongMonth() {
        Dates.Date(2018,13,1);
    }

    @Test(expected = Exception.class)
    public void testDateWrongDay() {
        Dates.Date(2018,2,29);
    }

    @Test(expected = Exception.class)
    public void testDateWrongYear() {
        Dates.Date(0,1,1);
    }

    @Test
    public void testToString() {
        assertNull(Dates.toString(null));
        assertEquals("07/12/1980", Dates.toString(new Date(80, 6, 12, 23, 59)));
        assertEquals("07/12/1980", Dates.toString(new Date(80, 6, 12)));
        assertEquals("12.07.1980", Dates.toString(new Date(80, 6, 12, 23, 59), "dd.MM.yyyy"));
        assertEquals("12-Jul-1980", Dates.toString(new Date(80, 6, 12), "dd-MMM-yyyy"));
    }

    @Test
    public void testToDate() throws Exception {
        assertNull(Dates.toDate(null));
        assertNull(Dates.toDate(""));
        assertNull(Dates.toDate(" "));
        assertNull(Dates.toDate("  \t  "));

        assertEquals(new Date(50, 11, 31), Dates.toDate("12/31/50"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("7/12/80"));
        assertEquals(new Date(138, 6, 12), Dates.toDate("7/12/38"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("7/12/080"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980"));

        assertNull(Dates.toDate(null, null));
        assertNull(Dates.toDate("", ""));
        assertNull(Dates.toDate(" ", " "));
        assertNull(Dates.toDate("  \t  ", "  \t"));

        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", null));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", ""));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", " "));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980", "  \t"));

        assertEquals(new Date(80, 6, 12), Dates.toDate("7/12/80", "MM/dd/yy"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("7/12/80", "M/d/yyyy"));
        assertEquals(new Date(-1820, 6, 12), Dates.toDate("07/12/0080", "M/d/yyyy"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("Date is: 12 Jul 1980 [+]", "'Date is: 'dd MMM yyyy"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("Date is: 12 Jul 1980 year", "'Date is: 'dd MMM yyyy 'year'"));
        assertEquals(new Date(80, 0, 1), Dates.toDate("Date: 1980 year", "'Date: 'yyyy 'year'"));
        assertEquals(new Date(70, 0, 19), Dates.toDate("Date: 19 days from the 1th January 1970", "'Date: 'd 'days'"));
    }

    @Test(expected = Exception.class)
    public void testToDateLetter() throws Exception {
        Dates.toDate("13/13/2013");
    }

    @Test(expected = Exception.class)
    public void testToDateWrongPattern() throws Exception {
        Dates.toDate("12/12/2013", "a");
    }
}
