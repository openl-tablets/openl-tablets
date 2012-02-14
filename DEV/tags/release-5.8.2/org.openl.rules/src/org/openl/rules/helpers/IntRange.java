/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import java.util.List;

import org.openl.OpenL;
import org.openl.domain.IntRangeDomain;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.meta.IntValue;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * The <code>IntRange</code> class stores range of integers. Examples : "1-3",
 * "2 .. 4", "123 ... 1000" (Important: using of ".." and "..." requires spaces
 * between numbers and separator).
 */
public class IntRange extends IntRangeDomain implements INumberRange {

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

    public IntRange(Integer number) {
        super(number, number);
    }
    
    public boolean contains(IntValue value) {
        return contains(value.intValue());
    }

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
        // TODO: Correct tokenizing in grammar.
        super(0, 0);
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
                    .run(openl, new StringSourceCodeModule(range, ""), SourceType.INT_RANGE);
        } finally {
            // Load old openl messages list. 
            //
            OpenLMessages.getCurrentInstance().clear();
            OpenLMessages.getCurrentInstance().addMessages(oldMessages);
        }

        min = res.getMin().intValue();
        if(res.getLeftBoundType() == BoundType.EXCLUDING){
            min++;
        }
        max = res.getMax().intValue();
        if(res.getRightBoundType() == BoundType.EXCLUDING){
            max--;
        }
    }
}
