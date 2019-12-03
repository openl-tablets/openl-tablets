package org.openl.rules.cmatch.matcher;

import java.util.Objects;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;

public class ClassMinMaxMatcher implements IMatcher {
    private final Class<?> clazz;
    private final boolean isMaxMode;

    public ClassMinMaxMatcher(Class<?> clazz, boolean isMaxMode) {
        this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
        this.isMaxMode = isMaxMode;

        if (!Comparable.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Must implement Comparable.");
        }
    }

    @Override
    public Object fromString(String checkValue) {
        IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(clazz);
        return converter.parse(checkValue, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean match(Object var, Object checkValue) {
        Comparable<Object> c1 = (Comparable<Object>) var;
        Comparable<Object> c2 = (Comparable<Object>) checkValue;

        int result = c1.compareTo(c2);

        return isMaxMode ? result <= 0 : result >= 0;
    }
}
