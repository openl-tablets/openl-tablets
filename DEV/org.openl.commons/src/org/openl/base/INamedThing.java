/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.base;

/**
 * @author snshor
 * 
 *         Wow, almost everything has a name.
 */
public interface INamedThing {

    static public INamedThing[] EMPTY = {};

    static public int SHORT = 0;
    static public int REGULAR = 1;
    static public int LONG = 2;

    String getDisplayName(int mode);

    String getName();

}
