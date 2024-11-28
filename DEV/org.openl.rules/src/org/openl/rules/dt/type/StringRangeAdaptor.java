package org.openl.rules.dt.type;

import org.openl.binding.impl.NumericComparableString;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.range.Range;

public final class StringRangeAdaptor implements IRangeAdaptor<StringRange, NumericComparableString> {

    private static final StringRangeAdaptor INSTANCE = new StringRangeAdaptor();

    private StringRangeAdaptor() {
    }

    public static IRangeAdaptor<StringRange, NumericComparableString> getInstance() {
        return INSTANCE;
    }

    @Override
    public NumericComparableString getMax(StringRange range) {
        if (range == null) {
            return null;
        }
        if (range.getType().right == Range.Bound.CLOSED) {
            return range.getUpperBound().incrementAndGet();
        } else {
            return range.getUpperBound();
        }
    }

    @Override
    public NumericComparableString getMin(StringRange range) {
        if (range == null) {
            return null;
        }
        if (range.getType().left == Range.Bound.OPEN) {
            return range.getLowerBound().incrementAndGet();
        }
        return range.getLowerBound();
    }

    @Override
    public NumericComparableString adaptValueType(Object value) {
        return value == null ? null : NumericComparableString.valueOf(value.toString());
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }
}
