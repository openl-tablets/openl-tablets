package org.openl.rules.cmatch.matcher;

import lombok.RequiredArgsConstructor;

import org.openl.rules.convertor.String2DataConvertorFactory;

@RequiredArgsConstructor
public class ClassMatchMatcher implements IMatcher {
    private final Class<?> clazz;

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
