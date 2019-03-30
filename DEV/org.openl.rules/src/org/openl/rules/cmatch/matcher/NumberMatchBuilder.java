package org.openl.rules.cmatch.matcher;

import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;

public class NumberMatchBuilder extends AMatcherMapBuilder<NumberMatchMatcher> {

    public NumberMatchBuilder() {
        add(new NumberMatchMatcher(Integer.class, IntRange.class), int.class);
        add(new NumberMatchMatcher(Double.class, DoubleRange.class), double.class);
        add(new NumberMatchMatcher(Long.class, IntRange.class), long.class);
        add(new NumberMatchMatcher(Float.class, DoubleRange.class), float.class);
    }

    protected void add(NumberMatchMatcher matcher, Class<?> primitiveClass) {
        put(matcher.getDirectClass(), matcher);
        put(primitiveClass, matcher);
    }

    @Override
    public String getName() {
        return OP_MATCH;
    }

}
