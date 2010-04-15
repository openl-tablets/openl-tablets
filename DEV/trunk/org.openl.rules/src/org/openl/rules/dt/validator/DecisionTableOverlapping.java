/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt.validator;

import org.openl.util.ArrayOfNamedValues;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public class DecisionTableOverlapping {

    private int[] rules;
    private ArrayOfNamedValues value;

    public DecisionTableOverlapping(int[] rules, ArrayOfNamedValues value) {
        this.rules = rules;
        this.value = value;
    }

    public int[] getRules() {
        return rules;
    }

    public ArrayOfNamedValues getValues() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Rules #%s overlap for {%s}", ArrayTool.asString(rules), value.toString());
    }

}
