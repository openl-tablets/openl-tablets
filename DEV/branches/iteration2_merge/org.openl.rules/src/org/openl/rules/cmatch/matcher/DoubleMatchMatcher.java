package org.openl.rules.cmatch.matcher;

import org.openl.rules.helpers.DoubleRange;
import org.openl.types.IOpenClass;

public class DoubleMatchMatcher extends ARangeMatch {
    protected Class<?> getDirectClass() {
        return Double.class;
    }

    protected Class<?> getRangeClass() {
        return DoubleRange.class;
    }

    public boolean isTypeSupported(IOpenClass type) {
        Class c = type.getInstanceClass();
        return (getDirectClass() == c || double.class == c);
    }

    public boolean match(Object var, Object checkValue) {
        if (checkValue instanceof DoubleRange) {
            DoubleRange range = (DoubleRange) checkValue;
            return range.containsNumber((Number)var);
        } else {
            return checkValue.equals(var);
        }
    }

}
