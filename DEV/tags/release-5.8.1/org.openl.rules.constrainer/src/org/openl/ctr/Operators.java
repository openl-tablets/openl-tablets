/*
 * Created on Sep 8, 2005
 *
 */
package org.openl.ctr;

import org.openl.ie.constrainer.IntExp;

/**
 * @author snshor
 */
public class Operators {
    static public IntExp multiply(IntExp e, int n) {
        return e.mul(n);
    }

}
