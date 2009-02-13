package org.openl.rules.cmatch.matcher;

import java.util.Date;

import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.types.IOpenClass;


public class DateMatchMatcher implements IMatcher {
    private static final Class CLAZZ = Date.class;

    public Object fromString(String checkValue) {
        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(CLAZZ);
        return convertor.parse(checkValue, null, null);
    }

    public String getName() {
        return OP_MATCH;
    }

    public boolean isTypeSupported(IOpenClass type) {
        return (type.getInstanceClass() == CLAZZ);
    }

    public boolean match(Object var, Object checkValue) {
        return checkValue.equals(var);
    }

}
