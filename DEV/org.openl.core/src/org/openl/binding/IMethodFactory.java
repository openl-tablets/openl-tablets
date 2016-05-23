/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.util.Iterator;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 */
public interface IMethodFactory {

    IOpenMethod getMatchingMethod(String name, IOpenClass[] params) throws AmbiguousMethodException;

    Iterable<IOpenMethod> methods(String name);
}
