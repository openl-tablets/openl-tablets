/**
 * Created Aug 23, 2007
 */
package org.openl.rules.helpers;

/**
 * @author snshor
 *
 */
public class InOrNotIn {
    private static final String IN = "IN";
    private static final String NOT_IN = "NOT IN";

    boolean in;

    public InOrNotIn(String str) {
        if (IN.equalsIgnoreCase(str)) {
            in = true;
        } else if (NOT_IN.equalsIgnoreCase(str)) {
            in = false;
        } else {
            throw new RuntimeException();
        }
    }

    public boolean booleanValue() {
        return in;
    }

    public boolean isIn() {
        return in;
    }

    public void setIn(boolean in) {
        this.in = in;
    }

}
