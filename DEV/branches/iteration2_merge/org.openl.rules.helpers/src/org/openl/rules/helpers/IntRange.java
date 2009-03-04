/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import org.openl.OpenL;
import org.openl.domain.IntRangeDomain;
import org.openl.syntax.impl.StringSourceCodeModule;
import org.openl.util.RangeWithBounds;

/**
 * Integer range.
 * 
 * @author snshor
 */
public class IntRange extends IntRangeDomain implements INumberRange {

    /**
     * Constructor for <code>IntRange</code>. Tries to parse range text with
     * variety of formats. Supported range formats: "<min number> - <max
     * number>" or "[<, <=, >, >=]<number>" or "<number>+" Also numbers can
     * be enhanced with $ sign and K,M,B, e.g. $1K = 1000 Any symbols at the end
     * are allowed to support expressions like ">=2 barrels", "6-8 km^2"
     * 
     * @param range
     */
    public IntRange(String range) {
        // Spaces between numbers and '-' are mandatory. Example: "1 - 2" -
        // correct "1-2" - wrong.
        // TODO: Correct tokenizing in grammar.
        super(0, 0);
        OpenL openl = OpenL.getInstance("org.openl.j");
        RangeWithBounds res = (RangeWithBounds) openl
                .evaluate(new StringSourceCodeModule(range, null), "range.literal");

        min = res.getMin().intValue();
        max = res.getMax().intValue();
    }

    /**
     * Constructor for <code>IntRange</code> with provided <code>min</code>
     * and <code>max</code> values.
     * 
     * @param min
     * @param max
     */
    public IntRange(int min, int max) {
        super(min, max);
        if (min > max) {
            throw new RuntimeException(max + " must be more or equal than " + min);
        }
    }

}
