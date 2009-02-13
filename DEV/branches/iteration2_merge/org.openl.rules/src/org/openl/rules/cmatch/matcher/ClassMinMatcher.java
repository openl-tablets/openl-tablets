package org.openl.rules.cmatch.matcher;

import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.types.IOpenClass;


public class ClassMinMatcher implements IMatcher {
    private final Class<?> clazz;

    public ClassMinMatcher(Class<?> clazz) {
        this.clazz = clazz;
        if (!Comparable.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Must implement Comparable!");
        }
    }

    public Object fromString(String checkValue) {
        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(clazz);
        return convertor.parse(checkValue, null, null);
    }

    public String getName() {
        return OP_MIN;
    }

    public boolean isTypeSupported(IOpenClass type) {
        return (type.getInstanceClass() == clazz);
    }

    public boolean match(Object var, Object checkValue) {
        Comparable<Object> c1 = Comparable.class.cast(var);
        Comparable<Object> c2 = Comparable.class.cast(checkValue);

        return (c1.compareTo(c2) >= 0);
    }
}
