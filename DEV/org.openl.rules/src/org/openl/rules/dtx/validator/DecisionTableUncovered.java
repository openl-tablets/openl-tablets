/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dtx.validator;

import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 *
 */
public class DecisionTableUncovered {

    private ArrayOfNamedValues values;

    public DecisionTableUncovered(ArrayOfNamedValues values) {
        this.values = values;
    }

    public ArrayOfNamedValues getValues() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }

}
