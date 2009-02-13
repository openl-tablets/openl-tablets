package org.openl.rules.cmatch.matcher;


public class PrimitiveMinMatcher extends APrimitiveMatcher {

    public PrimitiveMinMatcher(Class primitive, Class bigClass) {
        super(primitive, bigClass);
    }

    public String getName() {
        return OP_MIN;
    }

    @Override
    protected boolean isMatch(int compareResult) {
        return (compareResult >= 0);
    }
}
