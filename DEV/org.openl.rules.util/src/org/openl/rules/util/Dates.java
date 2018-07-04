package org.openl.rules.util;

import java.util.Date;

import org.openl.rules.util.dates.DateInterval;
import org.openl.rules.util.dates.DateInterval.Scale;
import org.openl.rules.util.dates.DateInterval.Unit;

/**
 * A set of util methods to work with dates.
 *
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Vladyslav Pikus
 */
public final class Dates {

    private Dates() { /* NON */ }

    /**
     * Calculate difference between two dates
     *
     * @param startDate start date
     * @param endDate end date
     * @param unitName method type
     * @return difference between two dates
     */
    public static Double dateDif(Date startDate, Date endDate, String unitName) {
        Unit unit = Unit.getUnit(unitName);
        if (unit == null) {
            throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }
        DateInterval interval = DateInterval.between(startDate, endDate);
        switch (unit) {
            case DAYS:
                return interval.toDays();
            case WEEKS:
                return interval.toWeeks(Scale.INT);
            case MONTHS:
                return interval.toMonths(Scale.INT);
            case YEARS:
                return interval.toYears(Scale.INT);
            case DAYS_EXCLUDE_MONTHS_AND_YEARS:
                return interval.toDaysExcludeYearsAndMonths();
            case MONTHS_EXCLUDE_YEARS:
                return interval.toMonthsExcludeYears(Scale.INT);
            case DAYS_EXCLUDE_YEARS:
                return interval.toDaysExcludeYears();
            // calculate fractional difference between dates
            case WEEKS_FRACTIONAL:
                return interval.toWeeks(Scale.FRAC);
            case MONTHS_FRACTIONAL:
                return interval.toMonths(Scale.FRAC);
            case YEARS_FRACTIONAL:
                return interval.toYears(Scale.FRAC);
            case MONTHS_FRACTIONAL_EXCLUDE_YEARS:
                return interval.toMonthsExcludeYears(Scale.FRAC);
            default:
                // should never be happened
                throw new IllegalArgumentException("Unsupported unit '" + unitName + "'");
        }
    }
}
