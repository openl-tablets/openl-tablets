/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * The <code>DoubleRange</code> class stores range of floats. Examples :
 * "1.2-3", "2 .. 4", "123.456 ... 1000.00001" (Important: using of ".." and
 * "..." requires spaces between numbers and separator).
 */
public class DoubleRange implements INumberRange {
    private double lowerBound = Double.MIN_VALUE;
    private double upperBound = Double.MAX_VALUE;

    private BoundType lowerBoundType;
    private BoundType upperBoundType;

    public DoubleRange(double lowerBound, double upperBound) {
        this(lowerBound, upperBound, BoundType.INCLUDING, BoundType.INCLUDING);
    }

    public DoubleRange(double lowerBound, double upperBound, BoundType lowerBoundType, BoundType upperBoundType) {
        if (lowerBound > upperBound) {
            throw new RuntimeException(upperBound + " must be more or equal than " + lowerBound);
        }
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerBoundType = lowerBoundType;
        this.upperBoundType = upperBoundType;
    }

    public DoubleRange(String range) {
        // TODO: Correct tokenizing in grammar.
        OpenL openl = OpenL.getInstance("org.openl.j");
        
        RangeWithBounds res;
        
     // Save current openl messages before range parser invocation to
        // avoid populating messages list with errors what are not refer to
        // appropriate table. Reason: input string doesn't contain required
        // information about source. 
        //
        List<OpenLMessage> oldMessages = OpenLMessages.getCurrentInstance().getMessages();
        
        try {
            OpenLMessages.getCurrentInstance().clear();
            res = (RangeWithBounds) OpenLManager
                    .run(openl, new StringSourceCodeModule(range, ""), SourceType.DOUBLE_RANGE);
        } finally {
            // Load old openl messages list. 
            //
            OpenLMessages.getCurrentInstance().clear();
            OpenLMessages.getCurrentInstance().addMessages(oldMessages);
        }
        
        lowerBound = res.getMin().doubleValue();
        lowerBoundType = res.getLeftBoundType();
        upperBound = res.getMax().doubleValue();
        upperBoundType = res.getRightBoundType();
    }

    /**
     * Compares lower bounds.
     * 
     * @param range the DoubleRange to be compared
     * @return a negative integer, zero, or a positive integer as lower bound of
     *         this range is less than, equal to, or greater than the lower
     *         bound of specified range.
     */
    public int compareLowerBound(DoubleRange range) {
        if (lowerBound < range.lowerBound) {
            return -1;
        } else if (lowerBound == range.lowerBound) {
            if (lowerBoundType == BoundType.INCLUDING && range.lowerBoundType == BoundType.EXCLUDING) {
                return -1;
            } else if (lowerBoundType == range.lowerBoundType) {
                return 0;
            }
        }
        return 1;
    }
    
    /**
     * Compares upper bounds.
     * 
     * @param range the DoubleRange to be compared
     * @return a negative integer, zero, or a positive integer as upper bound of
     *         this range is less than, equal to, or greater than the upper
     *         bound of specified range.
     */
    public int compareUpperBound(DoubleRange range) {
        if (upperBound < range.upperBound) {
            return -1;
        } else if (upperBound == range.upperBound) {
            if (upperBoundType == BoundType.INCLUDING && range.upperBoundType == BoundType.EXCLUDING) {
                return -1;
            } else if (upperBoundType == range.upperBoundType) {
                return 0;
            }
        }
        return 1;
    }
    
    public boolean contains(double x) {
        if (lowerBound < x && x < upperBound) {
            return true;
        } else if (x == lowerBound && lowerBoundType == BoundType.INCLUDING) {
            return true;
        } else if (x == upperBound && upperBoundType == BoundType.INCLUDING) {
            return true;
        }
        return false;
    }

    public boolean contains(DoubleRange range) {
        return compareLowerBound(range) <= 0 && compareUpperBound(range) >= 0;
    }

    public boolean containsNumber(Number num) {
        return contains(num.doubleValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DoubleRange)) {
            return false;
        }
        DoubleRange other = (DoubleRange) obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(lowerBound, other.lowerBound);
        builder.append(upperBound, other.upperBound);
        builder.append(upperBoundType, other.upperBoundType);
        builder.append(upperBoundType, other.upperBoundType);
        return builder.isEquals();
    }

    /**
     * @return Returns the lowerBound.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * @return Returns the upperBound.
     */
    public double getUpperBound() {
        return upperBound;
    }

    public DoubleRange intersect(DoubleRange range) {
        int lowerBoundComaring = compareLowerBound(range);
        int upperBoundComaring = compareUpperBound(range);
        
        double lowerBound = lowerBoundComaring > 0 ? this.lowerBound : range.lowerBound;
        BoundType lowerBoundType = lowerBoundComaring > 0 ? this.lowerBoundType : range.lowerBoundType;
        double upperBound = upperBoundComaring < 0 ? this.upperBound : range.upperBound;
        BoundType upperBoundType = upperBoundComaring < 0 ? this.upperBoundType : range.upperBoundType;
        return lowerBound > upperBound ? null : new DoubleRange(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    /**
     * @param lowerBound The lowerBound to set.
     */
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @param upperBound The upperBound to set.
     */
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public BoundType getLowerBoundType() {
        return lowerBoundType;
    }

    public void setLowerBoundType(BoundType lowerBoundType) {
        this.lowerBoundType = lowerBoundType;
    }

    public BoundType getUpperBoundType() {
        return upperBoundType;
    }

    public void setUpperBoundType(BoundType upperBoundType) {
        this.upperBoundType = upperBoundType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(lowerBoundType == BoundType.INCLUDING){
            builder.append('[');
        } else{
            builder.append('(');
        }
        builder.append(lowerBound + "; " + upperBound);
        if(upperBoundType == BoundType.INCLUDING){
            builder.append(']');
        } else{
            builder.append(')');
        }
        return builder.toString();
    }
}
