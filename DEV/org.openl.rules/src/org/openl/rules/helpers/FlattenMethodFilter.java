package org.openl.rules.helpers;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.MethodFilter;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * Implementation of {@link MethodFilter} for flatten method from {@link RulesUtils}. If the method is called with no
 * parameters then the flatten method must not be found.
 */
public class FlattenMethodFilter implements MethodFilter {

    @Override
    public boolean predicate(JavaOpenMethod javaOpenMethod, IOpenClass[] callParams, ICastFactory castFactory) {
        return callParams.length > 0;
    }
}
