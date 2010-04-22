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

    private int[] rulesIndexes;
    private ArrayOfNamedValues value;

    public DecisionTableOverlapping(int[] rulesIndexes, ArrayOfNamedValues value) {
        this.rulesIndexes = rulesIndexes;
        this.value = value;
    }

    public int[] getRulesIndexes() {
        return rulesIndexes;
    }

    public ArrayOfNamedValues getValues() {
        return value;
    }

    @Override
    public String toString() {        
        return String.format("Rules with # %s overlap for values: %s", ArrayTool.asString(rulesIndexes), value.toString());
    }

}
