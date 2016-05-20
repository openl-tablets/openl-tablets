package org.openl.util;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Created by dl on 7/8/14.
 */
public class DateToolTest {
    @Test
    public void testYearDiff() {
        assertEquals(0, DateTool.yearDiff(null, null));
        assertEquals(0, DateTool.yearDiff(new Date(), null));
        assertEquals(0, DateTool.yearDiff(null, new Date()));
        Calendar start = Calendar.getInstance();
        start.set(2013, 10, 5);
        Calendar end = Calendar.getInstance();
        end.set(2015, 11, 6);

        assertEquals(2, DateTool.yearDiff(end.getTime(), start.getTime()));
    }
}
