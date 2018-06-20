package org.openl.rules.util.dates;

/**
 * Stub for cases when impossible to calculate a difference between two dates. For example, when one day is @{code null}
 *
 * @author Vladyslav Pikus
 */
final class NullableInterval extends DateInterval {

    @Override
    public Double toDays() {
        return null;
    }

    @Override
    public Double toWeeks(Scale scale) {
        return null;
    }

    @Override
    public Double toMonths(Scale scale) {
        return null;
    }

    @Override
    public Double toYears(Scale scale) {
        return null;
    }

    @Override
    public Double toDaysExcludeYearsAndMonths() {
        return null;
    }

    @Override
    public Double toDaysExcludeYears() {
        return null;
    }

    @Override
    public Double toMonthsExcludeYears(Scale scale) {
        return null;
    }
}
