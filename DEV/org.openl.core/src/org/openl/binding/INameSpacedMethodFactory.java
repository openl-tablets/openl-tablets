/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public interface INameSpacedMethodFactory {

    IMethodCaller getMethodCaller(String namespace,
            String name,
            IOpenClass[] params,
            ICastFactory casts) throws AmbiguousMethodException;

    IOpenMethod[] getMethods(String namespace, String name);

}
