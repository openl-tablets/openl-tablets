package org.openl.rules.cmatch.matcher;

import org.openl.rules.convertor.String2DataConvertorFactory;

public class ClassMatchMatcher implements IMatcher {
    private final Class<?> clazz;

    public ClassMatchMatcher(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object fromString(String checkValue) {
        return String2DataConvertorFactory.getConvertor(clazz).parse(checkValue, null);
    }

    @Override
    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        return checkValue.equals(var);
    }
}
