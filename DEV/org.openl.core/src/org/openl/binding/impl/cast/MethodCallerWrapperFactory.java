package org.openl.binding.impl.cast;

import org.openl.binding.ICastFactory;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenMethod;

public interface MethodCallerWrapperFactory {
    IMethodCaller build(ICastFactory castFactory,
            IMethodCaller methodCaller,
            JavaOpenMethod javaOpenMethod,
            IOpenClass[] callParams);
}
