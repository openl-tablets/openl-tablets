package org.openl.rules.util.dates;

import java.util.Calendar;
import java.util.Date;

/**
 * Abstract class for calculation of difference between two dates in days, weeks, years
 *
 * @author Vladyslav Pikus
 */
public abstract class DateInterval {

    DateInterval() {
        /* NON */ }

    /**
     * Calculate a difference in full days. The result are always integer.
     *
     * Current implementation is an equivalent to Excel native functions DATEDIF(start, end, "D")
     *
     * @return difference in full
     */
    public abstract Double toDays();

    /**
     * Calculate a difference in weeks. If parameter has {@code Scale.INT} - the result will be calculated in full
     * weeks, otherwise will contain a fractional part of a last uncompleted week
     *
     * @param scale specifies a scale of the result
     * @return difference in weeks
     */
    public abstract Double toWeeks(Scale scale);

    /**
     * Calculate a difference in months. If parameter has {@code Scale.INT} - the result will be calculated in full
     * months, otherwise will contain a fractional part of a last uncompleted month
     *
     * Current implementation is an equivalent to Excel native functions DATEDIF(start, end, "M")
     *
     * @param scale specifies a scale of the result
     * @return difference in months
     */
    public abstract Double toMonths(Scale scale);

    /**
     * Calculate a difference in years. If parameter has {@code Scale.INT} - the result will be calculated in full
     * years, otherwise will contain a fractional part of a last uncompleted year
     *
     * Current implementation is an equivalent to Excel native functions DATEDIF(start, end, "Y")
     *
     * @param scale specifies a scale of the result
     * @return difference in years
     */
    public abstract Double toYears(Scale scale);

    /**
     * Calculate a difference in full days regarding last month. Current implementation is an equivalent to Excel native
     * functions DATEDIF(start, end, "MD")
     *
     * @return difference in days
     */
    public abstract Double toDaysExcludeYearsAndMonths();

    /**
     * Calculate a difference in full days regarding last year. Current implementation is an equivalent to Excel native
     * functions DATEDIF(start, end, "YD")
     *
     * @return difference in days
     */
    public abstract Double toDaysExcludeYears();

    /**
     * Calculate a difference in months regarding last year. If parameter has {@code Scale.INT} - the result will be
     * calculated in full months, otherwise will contain a fractional part of a last uncompleted month
     *
     * Current implementation is an equivalent to Excel native functions DATEDIF(start, end, "YM")
     *
     * @param scale specifies a scale of the result
     * @return difference in months
     */
    public abstract Double toMonthsExcludeYears(Scale scale);

    /**
     * Choose a suitable implementation for target date interval
     *
     * @param start start date
     * @param end end date
     * @return
     */
    public static DateInterval between(Date start, Date end) {
        if (start == null || end == null) {
            return DateIntervalImpl.NULLABLE;
        }
        return new DateIntervalImpl(start, end);
    }

    /**
     * Choose a suitable implementation for target date interval
     *
     * @param start start date
     * @param end end date
     * @return
     */
    public static DateInterval between(Calendar start, Calendar end) {
        if (start == null || end == null) {
            return DateIntervalImpl.NULLABLE;
        }
        return new DateIntervalImpl(start, end);
    }

    public enum Unit {
        YEARS("Y"),
        MONTHS("M"),
        WEEKS("W"),
        DAYS("D"),

        DAYS_EXCLUDE_MONTHS_AND_YEARS("MD"),
        MONTHS_EXCLUDE_YEARS("YM"),
        DAYS_EXCLUDE_YEARS("YD"),

        YEARS_FRACTIONAL("YF"),
        MONTHS_FRACTIONAL("MF"),
        WEEKS_FRACTIONAL("WF"),
        MONTHS_FRACTIONAL_EXCLUDE_YEARS("YMF");

        public static Unit getUnit(String unitName) {
            for (Unit unit : values()) {
                if (unit.unitName.equalsIgnoreCase(unitName)) {
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

    public enum Scale {
        INT,
        FRAC
    }
}
