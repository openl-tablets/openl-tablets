package org.openl.rules.cmatch.matcher;

import org.openl.types.IOpenClass;

public class StringMatchMatcher implements IMatcher {

    public Object fromString(String checkValue) {
        return checkValue;
    }

    public String getName() {
        return OP_MATCH;
    }

    public boolean isTypeSupported(IOpenClass type) {
        return (type.getInstanceClass() == String.class);
    }

    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        // hope both are Strings
        return checkValue.equals(var);
    }

}
