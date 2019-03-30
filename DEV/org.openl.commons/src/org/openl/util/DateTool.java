package org.openl.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.openl.rules.util.dates.DateInterval;

public class DateTool {

    public static final int MONTHS_IN_YEAR = 12;
    public static final int QUARTERS_IN_YEAR = 4;
    public static final int MONTHS_IN_QUARTER = 3;
    public static final int SECONDS_IN_DAY = 60 * 60 * 24;

    public static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

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

    public static Integer weekOfMonth(Date d) {
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.WEEK_OF_MONTH);
    }

    public static Integer weekOfYear(Date d) {
        if (d == null) {
            return null;
        }
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

        int lastDay = getLastDayOfMonth(c.getTime());

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

    /**
     * Converts a date to the String value according the dateFormat
     * 
     * @param date a date which should be converted
     * @param dateFormat
     * @return String date format
     */
    public static String dateToString(Date date, String dateFormat) {
        if (date == null) {
            return null;
        }
        DateFormat df = dateFormat == null ? DateFormat.getDateInstance(DateFormat.SHORT)
                                           : new SimpleDateFormat(dateFormat);
        return df.format(date);
    }

    /**
     * Converts a date to the String value according the default locale.
     * 
     * @param date
     * @return String date format
     */
    public static String dateToString(Date date) {
        return dateToString(date, null);
    }

    private static Integer castToInteger(Double d) {
        return d == null ? null : d.intValue();
    }

}
