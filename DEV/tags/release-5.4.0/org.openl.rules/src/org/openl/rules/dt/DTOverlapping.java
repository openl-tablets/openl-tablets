/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt;

import org.openl.util.ArrayOfNamedValues;
import org.openl.util.ArrayTool;

/**
 * @author snshor
 *
 */
public class DTOverlapping {

    int[] rules;

    ArrayOfNamedValues value;

    public DTOverlapping(int[] rules, ArrayOfNamedValues value) {
        this.rules = rules;
        this.value = value;
    }

    public int[] getRules() {
        return rules;
    }

    public ArrayOfNamedValues getValue() {
        return value;
    }

    @Override
    public String toString() {

        return "Rules #" + ArrayTool.asString(rules) + " overlap for {" + value + '}';
    }

}
