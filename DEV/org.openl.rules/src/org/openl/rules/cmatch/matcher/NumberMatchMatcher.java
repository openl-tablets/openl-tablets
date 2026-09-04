package org.openl.rules.cmatch.matcher;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.helpers.INumberRange;

@RequiredArgsConstructor
public class NumberMatchMatcher implements IMatcher {
    @Getter(AccessLevel.PROTECTED)
    private final Class<?> directClass;
    private final Class<?> rangeClass;

    @Override
    public Object fromString(String checkValue) {
        if (checkValue.length() == 0) {
            return null;
        }

        RuntimeException directParseException;
        try {
            IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(directClass);
            return convertor.parse(checkValue, null);
        } catch (RuntimeException e) {
            directParseException = e;
        }

        try {
            IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(rangeClass);
            return convertor.parse(checkValue, null);
        } catch (Exception e) {
            // throw exception from direct parsing
            throw directParseException;
        }
    }

    @Override
    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        if (checkValue instanceof INumberRange range) {
            return range.contains((Number) var);
        } else {
            return checkValue.equals(var);
        }
    }

}
