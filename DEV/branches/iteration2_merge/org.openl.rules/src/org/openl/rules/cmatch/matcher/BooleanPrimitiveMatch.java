package org.openl.rules.cmatch.matcher;

import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.types.IOpenClass;

public class BooleanPrimitiveMatch implements IMatcherBuilder, IMatcher {

    private final IString2DataConvertor convertor;

    public BooleanPrimitiveMatch() {
        convertor = String2DataConvertorFactory.getConvertor(boolean.class);
    }

    public IMatcher getInstanceIfSupports(IOpenClass type) {
        Class<?> c = type.getInstanceClass();
        if (c == boolean.class) {
            return this;
        } else {
            return null;
        }
    }

    public String getName() {
        return OP_MATCH;
    }

    public Object fromString(String checkValue) {
        return convertor.parse(checkValue, null, null);
    }

    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) return false;

        return checkValue.equals(var);
    }

}
