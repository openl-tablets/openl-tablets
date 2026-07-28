package org.openl.rules.cmatch.matcher;

import lombok.RequiredArgsConstructor;

import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

@RequiredArgsConstructor
public class ClassMinMaxBuilder implements IMatcherBuilder {

    private final boolean isMaxMode;

    @Override
    public IMatcher getInstanceIfSupports(IOpenClass type) {
        Class<?> c = type.getInstanceClass();
        if (ClassUtils.isAssignable(c, Comparable.class)) {
            return new ClassMinMaxMatcher(c, isMaxMode);
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return isMaxMode ? OP_MAX : OP_MIN;
    }
}
