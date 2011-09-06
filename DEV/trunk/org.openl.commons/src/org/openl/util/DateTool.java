package org.openl.util;

import java.util.Calendar;
import java.util.Date;

public class DateTool {

    public static final int MONTHS_IN_YEAR = 12;
    public static final int QUARTERS_IN_YEAR = 4;
    public static final int MONTHS_IN_QUARTER = 3;
    public static final int SECONDS_IN_DAY = 60 * 60 * 24;

    public static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

    public static int absMonth(Date d) {
        return year(d) * MONTHS_IN_YEAR + month(d);
    }

    public static int absQuarter(Date d) {
        return year(d) * QUARTERS_IN_YEAR + quarter(d);
    }

    public static int dayDiff(Date endDate, Date startDate) {        
        return DateDifference.getDifferenceInDays(endDate, startDate);
    }

    public static int dayOfMonth(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    public static Date firstDateOfQuarter(int absQuarter) {
        Calendar c = Calendar.getInstance();
        c.set(c.get(Calendar.YEAR), (absQuarter % QUARTERS_IN_YEAR) * (QUARTERS_IN_YEAR - 1), 1);
        return c.getTime();
    }

    public static Date lastDateOfQuarter(int absQuarter) {
        Calendar c = Calendar.getInstance();
        c.set(c.get(Calendar.YEAR), (absQuarter % QUARTERS_IN_YEAR) * MONTHS_IN_QUARTER + 2, 1);

        int lastDay = lastDayOfMonth(c.getTime());

        c.set(Calendar.DAY_OF_MONTH, lastDay);
        return c.getTime();
    }

    public static int lastDayOfMonth(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static int month(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.MONTH);
    }
    
    public static int monthDiff(Date endDate, Date startDate) {
        return DateDifference.getDifferenceInMonths(endDate, startDate);
    }
    
    public static int yearDiff(Date endDate, Date startDate) {
        return DateDifference.getDifferenceInYears(endDate, startDate);
    }
    
    public static int weekDiff(Date endDate, Date startDate) {
        return DateDifference.getDifferenceInWeeks(endDate, startDate);
    }

    public static int quarter(Date d) {
        return month(d) / 3;
    }

    public static int year(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.YEAR);
    }

}
