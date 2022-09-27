package org.openl.rules.cmatch.matcher;

import java.util.Comparator;
import java.util.Objects;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;

public class ClassMinMaxMatcher<T extends Comparable<? super T>> implements IMatcher {
    private final Class<T> clazz;
    private final boolean isMaxMode;

    public ClassMinMaxMatcher(Class<T> clazz, boolean isMaxMode) {
        this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
        this.isMaxMode = isMaxMode;
    }

    @Override
    public Object fromString(String checkValue) {
        IString2DataConvertor<T> converter = String2DataConvertorFactory.getConvertor(clazz);
        return converter.parse(checkValue, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean match(Object var, Object checkValue) {
        int result = Comparator.nullsFirst(Comparator.<T>naturalOrder()).compare((T)var, (T)checkValue);
        return isMaxMode ? (result <= 0) : (result >= 0);
    }
}
