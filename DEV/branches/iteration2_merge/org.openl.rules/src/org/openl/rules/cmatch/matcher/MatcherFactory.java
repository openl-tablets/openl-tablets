package org.openl.rules.cmatch.matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.types.IOpenClass;

public class MatcherFactory {
    private static final Map<String, List<IMatcher>> matchers = new HashMap<String, List<IMatcher>>();

    static {
        // = (match)
        registerMatcher(new StringMatchMatcher());
        registerMatcher(new IntegerMatchMatcher());
        registerMatcher(new DoubleMatchMatcher());
        registerMatcher(new DateMatchMatcher());

        // min
        registerMatcher(new MinMatcher());

        // max
        registerMatcher(new MaxMatcher());
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
