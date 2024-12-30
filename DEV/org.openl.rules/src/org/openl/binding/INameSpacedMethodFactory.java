package org.openl.binding;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author Yury Molchan
 */
public interface INameSpacedMethodFactory {

    IMethodCaller getMethodCaller(String namespace,
                                  String name,
                                  IOpenClass[] params,
                                  ICastFactory casts) throws AmbiguousMethodException;

}
