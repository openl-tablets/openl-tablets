package org.openl.rules.cmatch.matcher;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

public class ClassMatchBuilder implements IMatcherBuilder {

    @Override
    public IMatcher getInstanceIfSupports(IOpenClass type) {
        Class<?> c = type.getInstanceClass();
        if (ClassUtils.isAssignable(c, Comparable.class)) {
            IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(c);
            return new ClassMatchMatcher(c, convertor);
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return OP_MATCH;
    }

}
