/*
 * Created on Jul 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.base.INamedThing;

/**
 * @author snshor
 *
 */
public interface IOpenParameter extends INamedThing {
    public static int IN = 0, OUT = 1, INOUT = 2;

    public int getDirection();

    IOpenClass getType();

}
