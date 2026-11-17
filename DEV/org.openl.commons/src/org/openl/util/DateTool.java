package org.openl.util;

import java.util.Calendar;
import java.util.Date;

import org.openl.rules.util.dates.DateInterval;

public class DateTool {

    private DateTool() {
        // Utility class
    }

    public static final int MONTHS_IN_YEAR = 12;
    public static final int QUARTERS_IN_YEAR = 4;
    public static final int MONTHS_IN_QUARTER = 3;

    public static Integer absMonth(Date d) {
        if (d == null) {
            return null;
        }
        return getYear(d) * MONTHS_IN_YEAR + getMonth(d);
    }

    public static Integer absQuarter(Date d) {
        if (d == null) {
            return null;
        }
        return getYear(d) * QUARTERS_IN_YEAR + getQuarter(d);
    }

    public static Integer dayDiff(Date endDate, Date startDate) {
        return castToInteger(DateInterval.between(startDate, endDate).toDays());
    }

    public static Integer dayOfWeek(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    public static Integer dayOfMonth(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    public static Integer dayOfYear(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Returns the week of the month of a date, counted by ISO-8601 whatever the locale of the server.
     * <p>
     * A week starts on Monday. The first week of a month is the first one with at least four days in that month. The
     * days of a month before its first week are in week 0.
     *
     * @return the week from 0 to 5, or {@code null} for an empty date
     */
    public static Integer weekOfMonth(Date d) {
        return d == null ? null : isoWeeks(d).get(Calendar.WEEK_OF_MONTH);
    }

    /**
     * Returns the week of the year of a date, counted by ISO-8601 whatever the locale of the server.
     * <p>
     * A week starts on Monday. Week 1 is the week with the first Thursday of the year. So the last days of December can
     * be in week 1 of the next year, and the first days of January in week 52 or 53 of the previous year.
     *
     * @return the week from 1 to 53, or {@code null} for an empty date
     */
    public static Integer weekOfYear(Date d) {
        return d == null ? null : isoWeeks(d).get(Calendar.WEEK_OF_YEAR);
    }

    private static Calendar isoWeeks(Date d) {
        var c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setMinimalDaysInFirstWeek(4);
        c.setTime(d);
        return c;
    }

    public static Date firstDateOfQuarter(int absQuarter) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(absQuarter / QUARTERS_IN_YEAR, absQuarter % QUARTERS_IN_YEAR * (QUARTERS_IN_YEAR - 1), 1, 0, 0, 0);

        return c.getTime();
    }

    public static Date lastDateOfQuarter(int absQuarter) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(absQuarter / QUARTERS_IN_YEAR, absQuarter % QUARTERS_IN_YEAR * MONTHS_IN_QUARTER + 2, 1, 0, 0, 0);

        var lastDay = getLastDayOfMonth(c.getTime());

        c.set(Calendar.DAY_OF_MONTH, lastDay);
        return c.getTime();
    }

    public static Integer lastDayOfMonth(Date d) {
        if (d == null) {
            return null;
        }
        return getLastDayOfMonth(d);
    }

    private static int getLastDayOfMonth(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static Integer monthDiff(Date endDate, Date startDate) {
        return castToInteger(DateInterval.between(startDate, endDate).toMonths(DateInterval.Scale.INT));
    }

    public static Integer yearDiff(Date endDate, Date startDate) {
        return castToInteger(DateInterval.between(startDate, endDate).toYears(DateInterval.Scale.INT));
    }

    public static Integer weekDiff(Date endDate, Date startDate) {
        return castToInteger(DateInterval.between(startDate, endDate).toWeeks(DateInterval.Scale.FRAC));
    }

    public static Integer quarter(Date d) {
        if (d == null) {
            return null;
        }
        return getQuarter(d);
    }

    private static int getQuarter(Date d) {
        return getMonth(d) / 3;
    }

    public static Integer second(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.SECOND);
    }

    public static Integer minute(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.MINUTE);
    }

    /**
     * @param d Date
     * @return hour from 0 to 12
     */
    public static Integer hour(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.HOUR);
    }

    /**
     * @param d Date
     * @return hour from 0 to 24
     */
    public static Integer hourOfDay(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public static Integer month(Date d) {
        if (d == null) {
            return null;
        }
        return getMonth(d) + 1;
    }

    private static int getMonth(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.MONTH);
    }

    public static Integer year(Date d) {
        if (d == null) {
            return null;
        }
        return getYear(d);
    }

    private static int getYear(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.YEAR);
    }

    public static String amPm(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);

        if (c.get(Calendar.AM_PM) == Calendar.AM) {
            return "AM";
        } else {
            return "PM";
        }
    }

    private static Integer castToInteger(Double d) {
        return d == null ? null : d.intValue();
    }

}
