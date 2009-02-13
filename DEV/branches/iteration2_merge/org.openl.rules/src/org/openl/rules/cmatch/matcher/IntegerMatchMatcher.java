package org.openl.rules.cmatch.matcher;

import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;

public class IntegerMatchMatcher extends ARangeMatch {
    protected Class<?> getDirectClass() {
        return Integer.class;
    }

    protected Class<?> getRangeClass() {
        return IntRange.class;
    }

    public boolean isTypeSupported(IOpenClass type) {
        Class c = type.getInstanceClass();
        return (getDirectClass() == c || int.class == c);
    }

    public boolean match(Object var, Object checkValue) {
        return checkValue.equals(var);
    }
}
