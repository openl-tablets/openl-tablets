/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.domain.IntRangeDomain;
import org.openl.meta.IntValue;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * The <code>IntRange</code> class stores range of integers. Examples : "1-3",
 * "2 .. 4", "123 ... 1000" (Important: using of ".." and "..." requires spaces
 * between numbers and separator).
 */
@XmlRootElement
public class IntRange extends IntRangeDomain implements INumberRange {

    /**
     * Constructor for <code>IntRange</code> with provided <code>min</code>
     * and <code>max</code> values.
     */
    public IntRange(int min, int max) {
        super(min, max);
        if (min > max) {
            throw new RuntimeException(max + " must be more or equal than " + min);
        }
    }

    public IntRange(Integer number) {
        super(number, number);
    }

    public boolean contains(IntValue value) {
        return contains(value.intValue());
    }

    public IntRange() {
        super(0, 0);
    }
    
    /**
     * Constructor for <code>IntRange</code>. Tries to parse range text with
     * variety of formats. Supported range formats: "<min number> - <max
     * number>" or "[<, <=, >, >=]<number>" or "<number>+" Also numbers can
     * be enhanced with $ sign and K,M,B, e.g. $1K = 1000 Any symbols at the end
     * are allowed to support expressions like ">=2 barrels", "6-8 km^2"
     */
    public IntRange(String range) {
        super(0, 0);
        RangeWithBounds res = getRangeWithBounds(range);

        min = res.getMin().intValue();
        if (!res.getMin().equals(min)) {
            // For example, is converted from Long to Integer
            throw new IllegalArgumentException("Min value is out of int values range.");
        }
        if(res.getLeftBoundType() == BoundType.EXCLUDING){
            min++;
        }

        max = res.getMax().intValue();
        if (!res.getMax().equals(max)) {
            // For example, is converted from Long to Integer
            throw new IllegalArgumentException("Max value is out of int values range.");
        }
        if(res.getRightBoundType() == BoundType.EXCLUDING){
            max--;
        }
        if (min > max) {
            throw new RuntimeException(max + " must be more or equal than " + min);
        }
    }

    public static RangeWithBounds getRangeWithBounds(String range) {
        return IntRangeParser.getInstance().parse(range);
    }
}
