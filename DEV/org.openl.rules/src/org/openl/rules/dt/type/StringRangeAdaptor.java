package org.openl.rules.dt.type;

import org.openl.binding.impl.NumericComparableString;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.helpers.StringRange;

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
        if (range.getUpperBoundType() == BoundType.INCLUDING) {
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
        if (range.getLowerBoundType() == BoundType.EXCLUDING) {
            return range.getLowerBound().incrementAndGet();
        }
        return range.getLowerBound();
    }

    @Override
    public NumericComparableString adaptValueType(Object value) {
        return value == null ? null : NumericComparableString.valueOf(((CharSequence) value).toString());
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }
}
