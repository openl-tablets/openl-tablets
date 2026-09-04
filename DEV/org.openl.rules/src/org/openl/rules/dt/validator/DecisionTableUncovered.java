/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class DecisionTableUncovered {

    @Getter
    private final ArrayOfNamedValues values;

    @Override
    public String toString() {
        return values.toString();
    }

}
