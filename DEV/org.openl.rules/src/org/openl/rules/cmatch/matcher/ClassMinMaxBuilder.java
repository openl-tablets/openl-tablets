package org.openl.rules.cmatch.matcher;

import org.openl.types.IOpenClass;

public class ClassMinMaxBuilder implements IMatcherBuilder {

    private final Class<?> clazz;
    private final ClassMinMaxMatcher matcher;
    private final boolean isMaxMode;

    public static ClassMinMaxBuilder maxBuilder(Class<?> clazz) {
        return new ClassMinMaxBuilder(clazz, true);
    }

    public static ClassMinMaxBuilder minBuilder(Class<?> clazz) {
        return new ClassMinMaxBuilder(clazz, false);
    }

    public ClassMinMaxBuilder(Class<?> clazz, boolean isMaxMode) {
        this.clazz = clazz;
        this.isMaxMode = isMaxMode;
        matcher = new ClassMinMaxMatcher(clazz, isMaxMode);
    }

    @Override
    public IMatcher getInstanceIfSupports(IOpenClass type) {
        return clazz.equals(type.getInstanceClass()) ? matcher : null;
    }

    @Override
    public String getName() {
        return isMaxMode ? OP_MAX : OP_MIN;
    }
}
