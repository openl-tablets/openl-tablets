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
public interface IOpenMethodSignature extends INamedThing {

    IOpenParameter[] getParameters();

    IOpenClass getReturnType();

}
