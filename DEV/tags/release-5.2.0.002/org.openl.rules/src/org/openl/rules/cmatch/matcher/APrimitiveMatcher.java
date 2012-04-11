package org.openl.rules.cmatch.matcher;

import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.types.IOpenClass;

public abstract class APrimitiveMatcher implements IMatcher {
    private final Class<Object> primitive;
    private final Class<Comparable> bigClass;

    public APrimitiveMatcher(Class<Object> primitive, Class bigClass) {
        this.primitive = primitive;
        this.bigClass = bigClass;
    }

    public Object fromString(String checkValue) {
        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(primitive);
        return convertor.parse(checkValue, null, null);
    }

    public boolean isTypeSupported(IOpenClass type) {
        Class<?> c = type.getInstanceClass();
        return (primitive == c);
    }

    public boolean match(Object var, Object checkValue) {
        if (var == null || checkValue == null) return false;
        
        Comparable c1 = bigClass.cast(var);
        Comparable c2 = bigClass.cast(checkValue);
        return isMatch(c1.compareTo(c2));
    }

    protected abstract boolean isMatch(int compareResult);
}
