package org.openl.rules.dt.type;

import org.openl.rules.helpers.StringRange;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;

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
        if (max != null && range.getUpperBoundType() == BoundType.INCLUDING) {
            max = incLastChar(max);
        }
        return max;
    }

    @Override
    public String getMin(StringRange range) {
        if (range == null) {
            return null;
        }
        String min = range.getLowerBound();
        if (min != null && range.getLowerBoundType() == BoundType.EXCLUDING) {
            min = incLastChar(min);
        }
        return min;
    }

    private String incLastChar(String s) {
        StringBuilder sb = new StringBuilder(s);
        int lastChIdx = s.length() - 1;
        char lastCh = sb.charAt(lastChIdx);
        if (lastCh != Character.MAX_VALUE) {
            sb.setCharAt(lastChIdx, (char) (lastCh + 1));
        } else {
            sb.append(Character.MIN_VALUE);
        }
        return sb.toString();
    }

    @Override
    public String adaptValueType(Object value) {
        return value == null ? null : (String) value;
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }
}
