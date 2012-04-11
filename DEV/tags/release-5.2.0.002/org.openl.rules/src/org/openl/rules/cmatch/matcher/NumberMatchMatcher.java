package org.openl.rules.cmatch.matcher;

import org.openl.rules.helpers.INumberRange;
import org.openl.types.IOpenClass;

public class NumberMatchMatcher extends ARangeMatch {
    private final Class<?> primitive;

    public NumberMatchMatcher(Class<?> directClass, Class<?> rangeClass, Class<?> primitive) {
        super(directClass, rangeClass);
        this.primitive = primitive;
    }

    public boolean isTypeSupported(IOpenClass type) {
        Class<?> c = type.getInstanceClass();
        return (directClass == c || primitive == c);
    }

    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        if (checkValue instanceof INumberRange) {
            INumberRange range = (INumberRange) checkValue;
            return range.containsNumber((Number) var);
        } else {
            return checkValue.equals(var);
        }
    }

}
