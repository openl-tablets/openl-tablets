package org.openl.rules.cmatch.matcher;

import java.util.HashMap;
import java.util.Map;

import org.openl.types.IOpenClass;

public abstract class AMatcherMapBuilder<M extends IMatcher> implements IMatcherBuilder {
    private final Map<Class<?>, M> map;

    public AMatcherMapBuilder() {
        map = new HashMap<>();
    }

    @Override
    public IMatcher getInstanceIfSupports(IOpenClass type) {
        return map.get(type.getInstanceClass());
    }

    protected void put(Class<?> clazz, M matcher) {
        map.put(clazz, matcher);
    }
}
