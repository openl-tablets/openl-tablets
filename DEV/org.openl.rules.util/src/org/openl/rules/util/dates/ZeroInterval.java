package org.openl.rules.util.dates;

/**
 * Stub for cases when the result is always zero
 *
 * @author Vladyslav Pikus
 */
final class ZeroInterval extends DateInterval {

    @Override
    public Double toDays() {
        return 0.0d;
    }

    @Override
    public Double toWeeks(Scale scale) {
        return 0.0d;
    }

    @Override
    public Double toMonths(Scale scale) {
        return 0.0d;
    }

    @Override
    public Double toYears(Scale scale) {
        return 0.0d;
    }

    @Override
    public Double toDaysExcludeYearsAndMonths() {
        return 0.0d;
    }

    @Override
    public Double toDaysExcludeYears() {
        return 0.0d;
    }

    @Override
    public Double toMonthsExcludeYears(Scale scale) {
        return 0.0d;
    }

}
