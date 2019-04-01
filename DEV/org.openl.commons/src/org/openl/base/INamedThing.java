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

    INamedThing[] EMPTY = {};

    int SHORT = 0;
    int REGULAR = 1;
    int LONG = 2;

    String getDisplayName(int mode);

    String getName();

}
