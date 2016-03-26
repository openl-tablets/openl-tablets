package org.openl.extension.xmlrules.utils;

import java.util.Calendar;
import java.util.Date;

import org.openl.util.DateDifference;

public class DateFunctions {

    public static int diff(Object startDate, Object endDate, String unitName) {
        Unit unit = Unit.getUnit(unitName);

        if (unit == null) {
            throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }

        Calendar startCalendar = HelperFunctions.getCalendar(startDate);
        Calendar endCalendar = HelperFunctions.getCalendar(endDate);

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

    public static Date now() {
        return new Date();
    }

    public static int year(Object date) {
        Calendar calendar = HelperFunctions.getCalendar(date);
        return calendar.get(Calendar.YEAR);
    }

    public static int month(Object date) {
        Calendar calendar = HelperFunctions.getCalendar(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    public static int day(Object date) {
        Calendar calendar = HelperFunctions.getCalendar(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int hour(Object date) {
        Calendar calendar = HelperFunctions.getCalendar(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int minute(Object date) {
        Calendar calendar = HelperFunctions.getCalendar(date);
        return calendar.get(Calendar.MINUTE);
    }

    public static int second(Object date) {
        Calendar calendar = HelperFunctions.getCalendar(date);
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
