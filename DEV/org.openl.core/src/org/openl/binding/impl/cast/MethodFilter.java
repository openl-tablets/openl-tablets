package org.openl.binding.impl.cast;

import org.openl.binding.ICastFactory;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * This interface is designed to be used along with @{@link MethodSearchTuner} annotation. All methods marked
 * with @{@link MethodSearchTuner} with non default implementation for method filter can add addition logic to the
 * method search algorithms to skip method matching with used method parameter types. For example, if we don't want to
 * link the method with empty varargs variables.
 *
 * This filter removed the method from method search algorithms if predicate method return false.
 */
public interface MethodFilter {

    boolean predicate(JavaOpenMethod javaOpenMethod, IOpenClass[] callParams, ICastFactory castFactory);

}
