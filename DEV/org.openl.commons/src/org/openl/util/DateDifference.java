package org.openl.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Class with helper methods to find differences in two dates. Supports
 * differences in days, years, weeks and months. Correctly handles date
 * differences between dates before so called "the Epoch", namely January 1,
 * 1970, 00:00:00 GMT and dates after it.
 * 
 */
public class DateDifference {

    private static final double DAY_MILLIS = 1000 * 60 * 60 * 24.0015;
    private static final double WEEK_MILLIS = DAY_MILLIS * 7;
    private static final double MONTH_MILLIS = DAY_MILLIS * 30.43675;
    private static final double YEAR_MILLIS = WEEK_MILLIS * 52.2;

    private DateDifference() {
    }

    /**
     * Return the difference in days before endDate and startDate.
     * 
     * @param endDate the date that is considered to be the later one
     * @param startDate the date that is considered to be the earlier one
     * @return positive integer if endDate is greater than startDate, in other
     *         case the result will be negative.
     */
    public static Integer getDifferenceInDays(Date endDate, Date startDate) {
        return getDateDiff(Calendar.DATE, endDate, startDate);
    }

    /**
     * Return the difference in weeks before endDate and startDate.
     * 
     * @param endDate the date that is considered to be the later one
     * @param startDate the date that is considered to be the earlier one
     * @return positive integer if endDate is greater than startDate, in other
     *         case the result will be negative.
     */
    public static Integer getDifferenceInWeeks(Date endDate, Date startDate) {
        return getDateDiff(Calendar.WEEK_OF_YEAR, endDate, startDate);
    }

    /**
     * Return the difference in months before endDate and startDate.
     * 
     * @param endDate the date that is considered to be the later one
     * @param startDate the date that is considered to be the earlier one
     * @return positive integer if endDate is greater than startDate, in other
     *         case the result will be negative.
     */
    public static Integer getDifferenceInMonths(Date endDate, Date startDate) {
        return getDateDiff(Calendar.MONTH, endDate, startDate);
    }

    /**
     * Return the difference in years before endDate and startDate.
     * 
     * @param endDate the date that is considered to be the later one
     * @param startDate the date that is considered to be the earlier one
     * @return positive integer if endDate is greater than startDate, in other
     *         case the result will be negative.
     */
    public static Integer getDifferenceInYears(Date endDate, Date startDate) {
        return getDateDiff(Calendar.YEAR, endDate, startDate);
    }

    private static Integer getDateDiff(int calUnit, Date endDate, Date startDate) {
        if (endDate == null || startDate == null) {
            return null;
        }
        // swap if startDate later than endDate
        boolean neg = false;
        if (startDate.after(endDate)) {
            Date temp = startDate;
            startDate = endDate;
            endDate = temp;
            neg = true;
        }

        // estimate the diff. startDate is now guaranteed <= endDate
        int estimate = (int) getEstDiff(calUnit, startDate, endDate);

        // convert the Dates to GregorianCalendars
        GregorianCalendar calendarStartDate = new GregorianCalendar();
        calendarStartDate.setTime(startDate);
        GregorianCalendar calendarEndDate = new GregorianCalendar();
        calendarEndDate.setTime(endDate);

        // add 2 units less than the estimate to 1st date,
        // then serially add units till we exceed 2nd date
        calendarStartDate.add(calUnit, (int) estimate - 2);
        for (int i = estimate - 1;; i++) {
            calendarStartDate.add(calUnit, 1);
            if (calendarStartDate.after(calendarEndDate))
                return neg ? 1 - i : i - 1;
        }
    }

    private static int getEstDiff(int calUnit, Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        switch (calUnit) {
        // case Calendar.DAY_OF_WEEK_IN_MONTH:
            case Calendar.DAY_OF_MONTH:
                // case Calendar.DATE : // codes to same int as DAY_OF_MONTH
                return (int) (diff / DAY_MILLIS + .5);
            case Calendar.WEEK_OF_YEAR:
                return (int) (diff / WEEK_MILLIS + .5);
            case Calendar.MONTH:
                return (int) (diff / MONTH_MILLIS + .5);
            case Calendar.YEAR:
                return (int) (diff / YEAR_MILLIS + .5);
            default:
                return 0;
        }
    }

}
