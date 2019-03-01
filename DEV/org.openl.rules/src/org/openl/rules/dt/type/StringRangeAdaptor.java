package org.openl.rules.dt.type;

import org.openl.rules.helpers.StringRange;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.helpers.StringRangeParser;

public final class StringRangeAdaptor implements IRangeAdaptor<StringRange, String> {

    private static final StringRangeAdaptor INSTANCE = new StringRangeAdaptor();

    private StringRangeAdaptor() {
    }

    public static IRangeAdaptor<StringRange, String> getInstance() {
        return INSTANCE;
    }

    @Override
    public String getMax(StringRange range) {
        if (range == null) {
            return null;
        }
        String max = range.getUpperBound();
        if (!StringRangeParser.MAX_VALUE.equals(max) && range.getUpperBoundType() == BoundType.INCLUDING) {
            max = max + " ";
        }
        return max;
    }

    @Override
    public String getMin(StringRange range) {
        if (range == null) {
            return null;
        }
        String min = range.getLowerBound();
        if (!StringRangeParser.MAX_VALUE.equals(min) && range.getLowerBoundType() == BoundType.EXCLUDING) {
            min = min + " ";
        }
        return min;
    }

    @Override
    public String adaptValueType(Object value) {
        return value == null ? null : ((CharSequence) value).toString();
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }
}
