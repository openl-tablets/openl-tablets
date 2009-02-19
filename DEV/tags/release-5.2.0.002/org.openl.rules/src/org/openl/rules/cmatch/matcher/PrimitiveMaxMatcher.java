package org.openl.rules.cmatch.matcher;

public class PrimitiveMaxMatcher extends APrimitiveMatcher {

    public PrimitiveMaxMatcher(Class primitive, Class bigClass) {
        super(primitive, bigClass);
    }

    public String getName() {
        return OP_MAX;
    }

    @Override
    protected boolean isMatch(int compareResult) {
        return (compareResult <= 0);
    }

}
