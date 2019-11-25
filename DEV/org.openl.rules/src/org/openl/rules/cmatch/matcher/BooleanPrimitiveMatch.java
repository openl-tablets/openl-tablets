package org.openl.rules.cmatch.matcher;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.types.IOpenClass;

public class BooleanPrimitiveMatch implements IMatcherBuilder, IMatcher {

    private final IString2DataConvertor<?> convertor;

    public BooleanPrimitiveMatch() {
        convertor = String2DataConvertorFactory.getConvertor(boolean.class);
    }

    @Override
    public Object fromString(String checkValue) {
        return convertor.parse(checkValue, null);
    }

    @Override
    public IMatcher getInstanceIfSupports(IOpenClass type) {
        Class<?> c = type.getInstanceClass();
        if (boolean.class == c) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return OP_MATCH;
    }

    @Override
    public boolean match(Object var, Object checkValue) {
        if (checkValue == null) {
            return false;
        }

        return checkValue.equals(var);
    }

}
