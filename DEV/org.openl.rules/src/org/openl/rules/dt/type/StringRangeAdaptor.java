package org.openl.rules.dt.type;

import org.openl.binding.impl.NumericComparableString;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.helpers.StringRangeParser;

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
        NumericComparableString max = range.getUpperBound();
        if (!StringRangeParser.MAX_VALUE.equals(max) && range.getUpperBoundType() == BoundType.INCLUDING) {
            max = NumericComparableString.valueOf(max.getValue() + " ");
        }
        return max;
    }

    @Override
    public NumericComparableString getMin(StringRange range) {
        if (range == null) {
            return null;
        }
        NumericComparableString min = range.getLowerBound();
        if (!StringRangeParser.MAX_VALUE.equals(min) && range.getLowerBoundType() == BoundType.EXCLUDING) {
            min = NumericComparableString.valueOf(min.getValue() + " ");
        }
        return min;
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
