package org.openl.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by dl on 7/8/14.
 */
public class DateToolTest {
    @Test
    public void testYearDiff() {
        assertNull(DateTool.yearDiff(null, null));
        assertNull(DateTool.yearDiff(new Date(), null));
        assertNull(DateTool.yearDiff(null, new Date()));
        Calendar start = Calendar.getInstance();
        start.set(2013, 10, 5);
        Calendar end = Calendar.getInstance();
        end.set(2015, 11, 6);

        assertEquals(new Integer(2), DateTool.yearDiff(end.getTime(), start.getTime()));
    }
}
