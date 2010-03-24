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
    // public IMethodCaller getMethodCaller(String name, IOpenClass[] params,
    // ICastFactory casts)
    // throws AmbiguousMethodException;

    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params) throws AmbiguousMethodException;

    public Iterator<IOpenMethod> methods();

}
