/*
 * Created on Nov 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import org.openl.base.INamedThing;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public interface IParameterDeclaration extends INamedThing {

    IParameterDeclaration[] EMPTY = new IParameterDeclaration[0];

    IOpenClass getType();

    default IOpenSourceCodeModule getModule() {
        return null;
    }

    default void removeDebugInformation() {
        // do nothing
    }
}
