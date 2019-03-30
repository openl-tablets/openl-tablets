package org.openl.rules.cmatch.matcher;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.helpers.INumberRange;

public class NumberMatchMatcher implements IMatcher {
    private final Class<?> directClass;
    private final Class<?> rangeClass;

    public NumberMatchMatcher(Class<?> directClass, Class<?> rangeClass) {
        this.directClass = directClass;
        this.rangeClass = rangeClass;
    }

    @Override
    public Object fromString(String checkValue) {
        if (checkValue.length() == 0) {
            return null;
        }

        RuntimeException directParseException = null;
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

    protected Class<?> getDirectClass() {
        return directClass;
    }

    @Override
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
