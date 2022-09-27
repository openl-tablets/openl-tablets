package org.openl.rules.cmatch.matcher;

import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

public class ClassMatchBuilder implements IMatcherBuilder {

    @Override
    public IMatcher getInstanceIfSupports(IOpenClass type) {
        Class<?> c = type.getInstanceClass();
        if (ClassUtils.isAssignable(c, Comparable.class)) {
            return new ClassMatchMatcher(c);
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return OP_MATCH;
    }

}
