package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;

public class DatesTest {

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
        assertEquals(new Date(80, 6, 12), Dates.toDate("7/12/80"));
        assertEquals(new Date(80, 6, 12), Dates.toDate("07/12/1980"));
    }

    @Test(expected = Exception.class)
    public void testToDateLetter() throws Exception {
        Dates.toDate("13/13/2013");
    }
}
