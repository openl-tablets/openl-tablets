package org.openl.extension.xmlrules.utils;

import java.util.Calendar;
import java.util.Date;

import org.openl.rules.util.dates.DateInterval;
import org.openl.rules.util.dates.DateInterval.Scale;
import org.openl.rules.util.dates.DateInterval.Unit;

public class DateFunctions {

    public static Integer diff(Object startDate, Object endDate, String unitName) {
        Unit unit = Unit.getUnit(unitName);

        if (unit == null) {
            throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }

        Calendar startCalendar = HelperFunctions.getCalendar(startDate);
        Calendar endCalendar = HelperFunctions.getCalendar(endDate);

        DateInterval interval = DateInterval.between(startCalendar, endCalendar);
        switch (unit) {
            case YEARS:
                return castToInteger(interval.toYears(Scale.INT));
            case MONTHS:
                return castToInteger(interval.toMonths(Scale.INT));
            case DAYS:
                return castToInteger(interval.toDays());
            case DAYS_EXCLUDE_MONTHS_AND_YEARS:
                return castToInteger(interval.toDaysExcludeYearsAndMonths());
            case DAYS_EXCLUDE_YEARS:
                return castToInteger(interval.toDaysExcludeYears());
            case MONTHS_EXCLUDE_YEARS:
                return castToInteger(interval.toMonthsExcludeYears(Scale.INT));
            default:
                throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }
    }

    private static Integer castToInteger(Double d) {
        return d == null ? null : d.intValue();
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
}
