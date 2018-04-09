package org.openl.rules.dt.type;

import org.openl.rules.helpers.DoubleRange;
import org.openl.util.RangeWithBounds.BoundType;

public final class DoubleRangeAdaptor implements IRangeAdaptor<DoubleRange, Double> {
    private final static DoubleRangeAdaptor INSTANCE = new DoubleRangeAdaptor();
    
    private DoubleRangeAdaptor(){
    }
    
    public static IRangeAdaptor<DoubleRange, Double> getInstance(){
        return INSTANCE;
    }

    public Double getMax(DoubleRange range) {
        double max = range.getUpperBound();
        if (max != Double.POSITIVE_INFINITY && range.getUpperBoundType() == BoundType.INCLUDING) {
        	// the max should be moved to the right,
        	// to ensure that range.getUpperBound() will get to the interval
        	//
        	max += Math.ulp(max);
        }
        return max;
    }    
    
    public Double getMin(DoubleRange range) {
        double min = range.getLowerBound();
        if (range.getLowerBoundType() == BoundType.EXCLUDING) {
            min += Math.ulp(min);
        }
        return min;
    }


	@Override
	public Double adaptValueType(Object value) {
	    if (value == null){
	        return null;
	    }
        return Double.valueOf(((Number)value).doubleValue());
	}

	@Override
	public boolean useOriginalSource() {
		return false;
	}

	@Override
	public Class<?> getIndexType() {
		return Double.class;
	}
}
