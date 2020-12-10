/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt.validator;

import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 *
 */
public class DecisionTableUncovered {

    private final ArrayOfNamedValues values;

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
