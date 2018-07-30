package org.openl.rules.dt.type;

import org.openl.rules.helpers.IntRange;

public final class IntRangeAdaptor implements IRangeAdaptor<IntRange, Integer> {
    private final static IntRangeAdaptor INSTANCE = new IntRangeAdaptor();

    private IntRangeAdaptor() {
    }

    public static IRangeAdaptor<IntRange, Integer> getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getMax(IntRange range) {
        if (range == null) {
            return null;
        }
        
        int max = range.getMax();

        if (max != Integer.MAX_VALUE) {
            max = max + 1;
        }

        return Integer.valueOf(max);
    }

    /**
     * {@inheritDoc}
     */
    public Integer getMin(IntRange range) {
        if (range == null) {
            return null;
        }

        return Integer.valueOf(range.getMin());
    }


	@Override
	public Integer adaptValueType(Object value) {
	    if (value == null){
	        return null;
	    }
        return Integer.valueOf(((Number)value).intValue());
	}

	@Override
	public boolean useOriginalSource() {
		return false;
	}

	@Override
	public Class<?> getIndexType() {
		return Integer.class;
	}

}
