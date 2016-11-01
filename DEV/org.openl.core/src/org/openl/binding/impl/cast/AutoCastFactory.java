package org.openl.binding.impl.cast;

import org.openl.binding.IBindingContext;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

public interface AutoCastFactory {
    IMethodCaller build(IBindingContext bindingContext, IMethodCaller methodCaller, IOpenClass[] parameterTypes);
}
