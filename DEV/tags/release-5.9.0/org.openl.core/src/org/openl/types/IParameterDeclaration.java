/*
 * Created on Nov 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.base.INamedThing;

/**
 * @author snshor
 *
 */
public interface IParameterDeclaration extends INamedThing {

    int IN = 0;
    int OUT = 1;
    int INOUT = 2;

    int getDirection();

    IOpenClass getType();
}
