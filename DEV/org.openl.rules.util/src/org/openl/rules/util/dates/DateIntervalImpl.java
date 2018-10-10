package org.openl.rules.util.dates;

import static org.openl.rules.util.dates.CalendarWrapper.DAYS_IN_WEEK;
import static org.openl.rules.util.dates.CalendarWrapper.MONTH_IN_YEAR;

import java.util.Calendar;
import java.util.Date;

/**
 * Calculates difference between two dates. If {@code startDate > endDate} the result will be negative
 *
 * @author Vladyslav Pikus
 */
final class DateIntervalImpl extends DateInterval {

    static final DateInterval NULLABLE = new NullableInterval();

    private CalendarWrapper startDate;
    private CalendarWrapper endDate;
    private boolean isNegative;

    DateIntervalImpl(Date start, Date end) {
        if (start.after(end)) {
            this.startDate = new CalendarWrapper(end);
            this.endDate = new CalendarWrapper(start);
            this.isNegative = true;
        } else {
            this.startDate = new CalendarWrapper(start);
            this.endDate = new CalendarWrapper(end);
            this.isNegative = false;
        }
    }

    DateIntervalImpl(Calendar start, Calendar end) {
        if (start.after(end)) {
            this.startDate = new CalendarWrapper(end);
            this.endDate = new CalendarWrapper(start);
            this.isNegative = true;
        } else {
            this.startDate = new CalendarWrapper(start);
            this.endDate = new CalendarWrapper(end);
            this.isNegative = false;
        }
    }

    @Override
    public Double toDays() {
        double result = endDate.daysDiff(startDate);
        return toSignDouble(result);
    }

    @Override
    public Double toWeeks(Scale scale) {
        double result = ((double) endDate.daysDiff(startDate)) / DAYS_IN_WEEK;
        if (Scale.FRAC == scale) {
            return toSignDouble(result);
        }
        return toSignAndRemoveFraction(result);
    }

    @Override
    public Double toMonths(Scale scale) {
        int result = endDate.monthsDiff(startDate);
        if (Scale.INT == scale) {
            return toSignDouble(result);
        }

        double daysDif = endDate.daysDiffExcludeYearsAndMonths(startDate);
        int prevMonthL = getUncompleteMonthLength();
        return toSignDouble(result + daysDif / prevMonthL);
    }

    @Override
    public Double toYears(Scale scale) {
        int result = endDate.yearsDiff(startDate);
        if (Scale.INT == scale) {
            return toSignDouble(result);
        }

        double daysDif = endDate.daysDiffExcludeYears(startDate);
        int yearLen = endDate.getUncompleteYearLength(startDate);
        return toSignDouble(result + daysDif / yearLen);
    }

    @Override
    public Double toDaysExcludeYearsAndMonths() {
        int result = endDate.daysDiffExcludeYearsAndMonths(startDate);
        return toSignDouble(result);
    }

    @Override
    public Double toDaysExcludeYears() {
        int result = endDate.daysDiffExcludeYears(startDate);
        return toSignDouble(result);
    }

    @Override
    public Double toMonthsExcludeYears(Scale scale) {
        int result = endDate.monthsDiff(startDate) % MONTH_IN_YEAR;
        if (Scale.INT == scale) {
            return toSignDouble(result);
        }

        double daysDif = endDate.daysDiffExcludeYearsAndMonths(startDate);
        int prevMonthL = getUncompleteMonthLength();
        return toSignDouble(result + daysDif / prevMonthL);
    }

    /**
     * Adds a sign if it is necessary
     *
     * @param d target double
     * @return signed double
     */
    private double toSignDouble(double d) {
        if (d == 0.0d) {
            return d;
        }
        return isNegative ? -1 * d : d;
    }

    /**
     * Removes fractional part from double and add a sign if it is necessary
     *
     * @param d target double
     * @return signed double without fractional part
     */
    private double toSignAndRemoveFraction(double d) {
        return (int) toSignDouble(d);
    }

    private int getUncompleteMonthLength() {
        if (startDate.getDay() <= endDate.getDay()) {
            return endDate.getActualMonthLength();
        } else {
            return endDate.getPrevMonthLength();
        }
    }

}
