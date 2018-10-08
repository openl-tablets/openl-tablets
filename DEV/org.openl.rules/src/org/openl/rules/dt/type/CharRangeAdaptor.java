package org.openl.rules.dt.type;

import org.openl.rules.helpers.CharRange;

public final class CharRangeAdaptor implements IRangeAdaptor<CharRange, Character> {
    private final static CharRangeAdaptor INSTANCE = new CharRangeAdaptor();

    private CharRangeAdaptor() {
    }

    public static IRangeAdaptor<CharRange, Character> getInstance() {
        return INSTANCE;
    }

    public Character getMax(CharRange range) {
        if (range == null) {
            return null;
        }
        
        int max = range.getMax();
        
        if (max != Character.MAX_VALUE) {
            max = max + 1;
        }

        return new Character((char) max);
    }

    public Character getMin(CharRange range) {
        if (range == null) {
            return null;
        }

        return new Character((char) range.getMin());
    }

    @Override
    public Character adaptValueType(Object value) {
        if (value == null){
            return null;
        }
        return (Character) value;
    }

    @Override
    public boolean useOriginalSource() {
        return false;
    }

    @Override
    public Class<?> getIndexType() {
        return Character.class;
    }
}
