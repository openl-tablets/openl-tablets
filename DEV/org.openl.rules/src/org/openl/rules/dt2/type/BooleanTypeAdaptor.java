/**
 * Created Aug 23, 2007
 */
package org.openl.rules.dt2.type;

/**
 * @author snshor
 */
public class BooleanTypeAdaptor {

    public boolean extractBooleanValue(Object target) {
        return ((Boolean) target).booleanValue();
    }

}
