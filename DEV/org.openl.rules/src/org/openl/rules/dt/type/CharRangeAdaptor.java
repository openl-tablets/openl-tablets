package org.openl.rules.dt.type;

import org.openl.rules.helpers.CharRange;
import org.openl.rules.range.Range;

public final class CharRangeAdaptor implements IRangeAdaptor<CharRange, Character> {
    private static final CharRangeAdaptor INSTANCE = new CharRangeAdaptor();

    private CharRangeAdaptor() {
    }

    public static IRangeAdaptor<CharRange, Character> getInstance() {
        return INSTANCE;
    }

    @Override
    public Character getMax(CharRange range) {
        if (range == null) {
            return null;
        }

        Character max = range.getMax();

        if (max != Character.MAX_VALUE && range.getType().right == Range.Bound.CLOSED) {
            max++;
        }

        return max;
    }

    @Override
    public Character getMin(CharRange range) {
        if (range == null) {
            return null;
        }

        Character min = range.getMin();
        if (range.getType().left == Range.Bound.OPEN) {
            min++;
        }
        return min;
    }

    @Override
    public Character adaptValueType(Object value) {
        if (value == null) {
            return null;
        }
        return (Character) value;
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }

}
