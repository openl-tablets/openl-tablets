package org.openl.rules.dt2.type;

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
        return Integer.valueOf(range.getMin());
    }


	@Override
	public Integer adaptValueType(Object value) {
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
