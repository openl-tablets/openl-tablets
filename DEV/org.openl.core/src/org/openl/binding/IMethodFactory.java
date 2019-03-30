/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 */
public interface IMethodFactory {

    IOpenMethod getMethod(String name, IOpenClass[] params);

    IOpenMethod getConstructor(IOpenClass[] params);

    Iterable<IOpenMethod> methods(String name);

    Iterable<IOpenMethod> constructors();

}
