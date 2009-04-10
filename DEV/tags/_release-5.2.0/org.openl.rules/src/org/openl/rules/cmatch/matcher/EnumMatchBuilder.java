package org.openl.rules.cmatch.matcher;

import org.openl.types.IOpenClass;

public class EnumMatchBuilder implements IMatcherBuilder {

    public IMatcher getInstanceIfSupports(IOpenClass type) {
        Class<?> clazz = type.getInstanceClass();
        return (clazz.isEnum()) ? new EnumMatchMatcher(clazz) : null;
    }

    public String getName() {
        return OP_MATCH;
    }

}
