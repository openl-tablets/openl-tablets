/**
 * Created Aug 23, 2007
 */
package org.openl.rules.helpers;

/**
 * @author snshor
 *
 */
public class InOrNotIn {
    static final String IN = "IN", NOT_IN = "NOT IN";

    boolean in;

    public InOrNotIn(String str) {
        if (str.toUpperCase().equals(IN)) {
            in = true;
        } else if (str.toUpperCase().equals(NOT_IN)) {
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
