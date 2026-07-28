/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt.validator;

import lombok.RequiredArgsConstructor;

import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class DecisionTableUncovered {

    private final ArrayOfNamedValues values;

    public ArrayOfNamedValues getValues() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }

}
