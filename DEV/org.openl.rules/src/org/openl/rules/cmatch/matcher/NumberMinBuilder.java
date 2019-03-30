package org.openl.rules.cmatch.matcher;

public class NumberMinBuilder extends AMatcherMapBuilder<ClassMinMaxMatcher> {

    public NumberMinBuilder() {
        add(Integer.class, int.class);
        add(Double.class, double.class);
        add(Long.class, long.class);
        add(Float.class, float.class);
    }

    private void add(Class<?> bigClass, Class<?> primitiveClass) {
        ClassMinMaxMatcher matcher = new ClassMinMaxMatcher(bigClass, false);
        put(bigClass, matcher);
        put(primitiveClass, matcher);
    }

    @Override
    public String getName() {
        return OP_MIN;
    }
}
