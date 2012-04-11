package org.openl.rules.cmatch.matcher;

import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.types.IOpenClass;


public class ClassMatchMatcher implements IMatcher {
    private final Class<?> clazz;

    public ClassMatchMatcher(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Object fromString(String checkValue) {
        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(clazz);
        return convertor.parse(checkValue, null, null);
    }

    public String getName() {
        return OP_MATCH;
    }

    public boolean isTypeSupported(IOpenClass type) {
        return (type.getInstanceClass() == clazz);
    }

    public boolean match(Object var, Object checkValue) {
        return checkValue.equals(var);
    }

}
