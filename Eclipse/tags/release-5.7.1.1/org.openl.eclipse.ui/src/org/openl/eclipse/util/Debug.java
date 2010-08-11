/*
 * Created on Jul 24, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import org.openl.util.Log;

/**
 * Compile time Debug logger.
 */
public class Debug {
    static final public boolean DEBUG = true;

    static public void debug(String s) {
        if (DEBUG) {
            Log.debug(s);
            System.out.println(s);
        }
    }

    static public void debug(String s, Throwable t) {
        if (DEBUG) {
            Log.debug(s, t);
            System.out.println(s);
            System.out.println(t.getMessage());
        }
    }

}
