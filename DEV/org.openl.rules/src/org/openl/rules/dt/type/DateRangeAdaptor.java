package org.openl.rules.dt.type;

import org.openl.rules.helpers.DateRange;

import java.util.Date;

public class DateRangeAdaptor implements IRangeAdaptor<DateRange, Long> {

    private static final class InstanceHolder {
        private static final DateRangeAdaptor INSTANCE = new DateRangeAdaptor();
    }

    private DateRangeAdaptor() {
    }

    public static IRangeAdaptor<DateRange, Long> getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Long getMax(DateRange range) {
        if (range == null) {
            return null;
        }
        long max = range.getUpperBound();
        if (max != Long.MAX_VALUE) {
            max += 1;
        }
        return max;
    }

    @Override
    public Long getMin(DateRange range) {
        if (range == null) {
            return null;
        }
        return range.getLowerBound();
    }

    @Override
    public Long adaptValueType(Object value) {
        return value == null ? null : ((Date) value).getTime();
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }
}
