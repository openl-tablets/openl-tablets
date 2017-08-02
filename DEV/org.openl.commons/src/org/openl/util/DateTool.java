package org.openl.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

    public static int dayOfWeek(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    public static int dayOfMonth(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    public static int dayOfYear(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_YEAR);
    }

    public static int weekOfMonth(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.WEEK_OF_MONTH);
    }

    public static int weekOfYear(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.WEEK_OF_YEAR);
    }

    public static Date firstDateOfQuarter(int absQuarter) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(absQuarter / QUARTERS_IN_YEAR, (absQuarter % QUARTERS_IN_YEAR) * (QUARTERS_IN_YEAR - 1), 1, 0, 0, 0);

        return c.getTime();
    }

    public static Date lastDateOfQuarter(int absQuarter) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(absQuarter / QUARTERS_IN_YEAR, (absQuarter % QUARTERS_IN_YEAR) * MONTHS_IN_QUARTER + 2, 1, 0, 0, 0);

        int lastDay = lastDayOfMonth(c.getTime());

        c.set(Calendar.DAY_OF_MONTH, lastDay);
        return c.getTime();
    }

    public static int lastDayOfMonth(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
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

    public static int second(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.SECOND);
    }

    public static int minute(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.MINUTE);
    }

    /**
     * @param d Date
     * @return hour from 0 to 12
     */
    public static int hour(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.HOUR);
    }

    /**
     * @param d Date
     * @return hour from 0 to 24
     */
    public static int hourOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public static int month(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.MONTH);
    }

    public static int year(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.YEAR);
    }

    public static String amPm(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);

        if (c.get(Calendar.AM_PM) == Calendar.AM) {
            return "AM";
        } else {
            return "PM";
        }
    }

    /**
     * Converts a date to the String value according the dateFormat
     * 
     * @param date a date which should be converted
     * @param dateFormat
     * @return String date format
     */
    public static String dateToString(Date date, String dateFormat) throws Exception {
        DateFormat df = dateFormat == null ? DateFormat.getDateInstance(DateFormat.SHORT) : new SimpleDateFormat(dateFormat);
        return df.format(date);
    }

    /**
     * Converts a date to the String value according the default locale.
     * 
     * @param date
     * @return String date format
     * @throws Exception
     */
    public static String dateToString(Date date) throws Exception {
        return dateToString(date, null);
    }
}
