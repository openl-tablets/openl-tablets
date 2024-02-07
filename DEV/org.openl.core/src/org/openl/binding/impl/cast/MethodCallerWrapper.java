package org.openl.binding.impl.cast;

import org.openl.binding.ICastFactory;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * This interface is designed to be used along with @{@link MethodSearchTuner} annotation. All methods marked
 * with @{@link MethodSearchTuner} with non default implementation can add addition logic to the method by wrapping
 * {@link org.openl.types.impl.MethodCaller} with additional logic.
 */
public interface MethodCallerWrapper {

    IMethodCaller handle(IMethodCaller methodCaller,
                         JavaOpenMethod javaOpenMethod,
                         IOpenClass[] callParams,
                         ICastFactory castFactory);
}
