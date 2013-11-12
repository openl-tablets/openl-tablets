package org.openl.rules.cmatch.matcher;

import org.openl.rules.convertor.IString2DataConvertor;

public class ClassMatchMatcher implements IMatcher {
    private final Class<?> clazz;
    private final IString2DataConvertor convertor;

    public ClassMatchMatcher(Class<?> clazz, IString2DataConvertor convertor) {
        this.clazz = clazz;
        this.convertor = convertor;
    }

    public Object fromString(String checkValue) {
        return convertor.parse(checkValue, null, null);
    }

    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        return checkValue.equals(var);
    }

    public Class<?> getClazz() {
        return clazz;
    }

}
