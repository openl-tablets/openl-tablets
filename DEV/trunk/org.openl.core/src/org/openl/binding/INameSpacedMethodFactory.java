/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface INameSpacedMethodFactory {

    IMethodCaller getMethodCaller(String namespace, String name, IOpenClass[] params, ICastFactory casts)
            throws AmbiguousMethodException;

}
