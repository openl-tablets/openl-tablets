package org.openl.extension.xmlrules.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.DateUtil;
import org.openl.util.DateDifference;

public class DateFunctions {
    private static Pattern DEFAULT_DATE_PATTERN = Pattern.compile("(\\d{4})[-/\\.](\\d{1,2})[-/\\.](\\d{1,2})(\\s+(\\d{1,2}):(\\d{1,2})(:(\\d{1,2})(\\.(\\d+))?)?)?");
    private static Pattern US_DATE_PATTERN = Pattern.compile("(\\d{1,2})[-/\\.](\\d{1,2})[-/\\.](\\d{4})(\\s+(\\d{1,2}):(\\d{1,2})(:(\\d{1,2})(\\.(\\d+))?)?)?");

    public static int diff(Object startDate, Object endDate, String unitName) {
        Unit unit = Unit.getUnit(unitName);

        if (unit == null) {
            throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }

        Calendar startCalendar = getCalendar(startDate);
        Calendar endCalendar = getCalendar(endDate);

        switch (unit) {
            case YEARS:
                return DateDifference.getDifferenceInYears(endCalendar.getTime(), startCalendar.getTime());
            case MONTHS:
                return DateDifference.getDifferenceInMonths(endCalendar.getTime(), startCalendar.getTime());
            case DAYS:
                return DateDifference.getDifferenceInDays(endCalendar.getTime(), startCalendar.getTime());
            case DAYS_WITHOUT_MONTHS_AND_YEARS:
                startCalendar.set(Calendar.YEAR, endCalendar.get(Calendar.YEAR));
                startCalendar.set(Calendar.MONTH, endCalendar.get(Calendar.MONTH));
                return DateDifference.getDifferenceInDays(endCalendar.getTime(), startCalendar.getTime());
            case DAYS_WITHOUT_YEARS:
                startCalendar.set(Calendar.YEAR, endCalendar.get(Calendar.YEAR));
                return DateDifference.getDifferenceInDays(endCalendar.getTime(), startCalendar.getTime());
            case MONTHS_WITHOUT_YEARS:
                startCalendar.set(Calendar.YEAR, endCalendar.get(Calendar.YEAR));
                return DateDifference.getDifferenceInMonths(endCalendar.getTime(), startCalendar.getTime());
            default:
                throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }
    }

    private static Calendar getCalendar(Object date) {
        if (date instanceof Double) {
            return DateUtil.getJavaCalendar((Double) date);
        } else if (date instanceof Integer) {
            return DateUtil.getJavaCalendar((Integer) date);
        } else if (date instanceof String) {
            try {
                return DateUtil.getJavaCalendar(Double.parseDouble((String) date));
            } catch (NumberFormatException e) {
                Matcher matcher = DEFAULT_DATE_PATTERN.matcher((CharSequence) date);
                if (!matcher.matches()) {
                    matcher = US_DATE_PATTERN.matcher((CharSequence) date);
                }
                if (matcher.matches()) {
                    Calendar calendar = new GregorianCalendar();
                    calendar.set(Calendar.YEAR, Integer.parseInt(matcher.group(1)));
                    calendar.set(Calendar.MONTH, Integer.parseInt(matcher.group(2)) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(3)));

                    String hour = matcher.group(5);
                    calendar.set(Calendar.HOUR_OF_DAY, hour != null ? Integer.parseInt(hour) : 0);
                    String minute = matcher.group(6);
                    calendar.set(Calendar.MINUTE, minute != null ? Integer.parseInt(minute) : 0);
                    String second = matcher.group(8);
                    calendar.set(Calendar.SECOND, second != null ? Integer.parseInt(second) : 0);
                    String millisecond = matcher.group(10);
                    calendar.set(Calendar.MILLISECOND, millisecond != null ? Integer.parseInt(millisecond) : 0);
                    return calendar;
                }
            }
        } else if (date instanceof Date) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime((Date) date);
            return calendar;
        } else if (date instanceof String[][]) {
            String[][] dateArray = (String[][]) date;
            if (dateArray.length > 0 && dateArray[0].length > 0) {
                return getCalendar(dateArray[0][0]);
            }
        }

        throw new IllegalArgumentException("Unsupported date format '" + date + "'");
    }

    public static Date now() {
        return new Date();
    }

    public static int year(Object date) {
        Calendar calendar = getCalendar(date);
        return calendar.get(Calendar.YEAR);
    }

    public static int month(Object date) {
        Calendar calendar = getCalendar(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    public static int day(Object date) {
        Calendar calendar = getCalendar(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int hour(Object date) {
        Calendar calendar = getCalendar(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int minute(Object date) {
        Calendar calendar = getCalendar(date);
        return calendar.get(Calendar.MINUTE);
    }

    public static int second(Object date) {
        Calendar calendar = getCalendar(date);
        return calendar.get(Calendar.SECOND);
    }

    public static Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private enum Unit {
        YEARS("Y"),
        MONTHS("M"),
        DAYS("D"),
        DAYS_WITHOUT_MONTHS_AND_YEARS("MD"),
        MONTHS_WITHOUT_YEARS("YM"),
        DAYS_WITHOUT_YEARS("YD");

        public static Unit getUnit(String unitName) {
            for (Unit unit : values()) {
                if (unit.unitName.equals(unitName)) {
                    return unit;
                }
            }

            return null;
        }

        private final String unitName;

        Unit(String unitName) {
            this.unitName = unitName;
        }
    }
}
