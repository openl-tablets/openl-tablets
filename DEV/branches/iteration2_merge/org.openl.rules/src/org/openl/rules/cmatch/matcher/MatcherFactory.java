package org.openl.rules.cmatch.matcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;

public class MatcherFactory {
    private static final Map<String, List<IMatcher>> matchers = new HashMap<String, List<IMatcher>>();

    static {
        // = (match)
        registerMatcher(new StringMatchMatcher());
        registerMatcher(new NumberMatchMatcher(Integer.class, IntRange.class, int.class));
        registerMatcher(new NumberMatchMatcher(Double.class, DoubleRange.class, double.class));
        registerMatcher(new ClassMatchMatcher(Date.class));
        // ???
        registerMatcher(new NumberMatchMatcher(Long.class, IntRange.class, long.class));
        // ???
        registerMatcher(new NumberMatchMatcher(Float.class, DoubleRange.class, float.class));

        // min
        registerMatcher(new PrimitiveMinMatcher(int.class, Integer.class));
        registerMatcher(new PrimitiveMinMatcher(double.class, Double.class));
        registerMatcher(new PrimitiveMinMatcher(long.class, Long.class));
        registerMatcher(new PrimitiveMinMatcher(float.class, Float.class));
        registerMatcher(new ClassMinMatcher(Integer.class));
        registerMatcher(new ClassMinMatcher(Double.class));
        registerMatcher(new ClassMinMatcher(Long.class));
        registerMatcher(new ClassMinMatcher(Float.class));
        registerMatcher(new ClassMinMatcher(Date.class));

        // max
        registerMatcher(new PrimitiveMaxMatcher(int.class, Integer.class));
        registerMatcher(new PrimitiveMaxMatcher(double.class, Double.class));
        registerMatcher(new PrimitiveMaxMatcher(long.class, Long.class));
        registerMatcher(new PrimitiveMaxMatcher(float.class, Float.class));
        registerMatcher(new ClassMaxMatcher(Integer.class));
        registerMatcher(new ClassMaxMatcher(Double.class));
        registerMatcher(new ClassMaxMatcher(Long.class));
        registerMatcher(new ClassMaxMatcher(Float.class));
        registerMatcher(new ClassMaxMatcher(Date.class));
    }

    public static boolean hasMatcher(String name) {
        return (matchers.get(name) != null);
    }

    public static IMatcher getMatcher(String operationName, IOpenClass type) {
        List<IMatcher> m2 = matchers.get(operationName);
        if (m2 == null)
            return null;

        for (IMatcher matcher : m2) {
            if (matcher.isTypeSupported(type)) {
                return matcher;
            }
        }

        return null;
    }

    public static void registerMatcher(IMatcher matcher) {
        String operationName = matcher.getName();
        List<IMatcher> m2 = matchers.get(operationName);

        if (m2 == null) {
            m2 = new ArrayList<IMatcher>();
            matchers.put(operationName, m2);
        } else {
            if (m2.contains(matcher)) {
                throw new IllegalArgumentException("Matcher was already added!");
            }
        }

        m2.add(matcher);
    }
}
