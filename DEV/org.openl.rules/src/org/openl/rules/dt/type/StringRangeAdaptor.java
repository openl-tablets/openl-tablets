package org.openl.rules.dt.type;

import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.helpers.StringRangeParser;
import org.openl.rules.helpers.StringRangeValue;

public final class StringRangeAdaptor implements IRangeAdaptor<StringRange, StringRangeValue> {

    private static final StringRangeAdaptor INSTANCE = new StringRangeAdaptor();

    private StringRangeAdaptor() {
    }

    public static IRangeAdaptor<StringRange, StringRangeValue> getInstance() {
        return INSTANCE;
    }

    @Override
    public StringRangeValue getMax(StringRange range) {
        if (range == null) {
            return null;
        }
        StringRangeValue max = range.getUpperBound();
        if (!StringRangeParser.MAX_VALUE.equals(max) && range.getUpperBoundType() == BoundType.INCLUDING) {
            max = StringRangeValue.valueOf(max.getValue() + " ");
        }
        return max;
    }

    @Override
    public StringRangeValue getMin(StringRange range) {
        if (range == null) {
            return null;
        }
        StringRangeValue min = range.getLowerBound();
        if (!StringRangeParser.MAX_VALUE.equals(min) && range.getLowerBoundType() == BoundType.EXCLUDING) {
            min = StringRangeValue.valueOf(min.getValue() + " ");
        }
        return min;
    }

    @Override
    public StringRangeValue adaptValueType(Object value) {
        return value == null ? null : StringRangeValue.valueOf(((CharSequence) value).toString());
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }
}
