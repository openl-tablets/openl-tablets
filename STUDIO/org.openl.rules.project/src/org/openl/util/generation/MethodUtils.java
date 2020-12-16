package org.openl.util.generation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class MethodUtils {
    private MethodUtils() {
    }

    private static final Comparator<Method> METHOD_COMPARATOR = Comparator.comparing(Method::getName)
        .thenComparingInt(Method::getParameterCount)
        .thenComparing(Method::getParameterTypes, MethodUtils::compareNames);

    private static int compareNames(Class<?>[] p1, Class<?>[] p2) {
        for (int i = 0; i < p1.length; i++) {
            String name1 = p1[i].getName();
            String name2 = p2[i].getName();
            int cmp = name1.compareTo(name2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public static List<Method> sort(Collection<Method> m) {
        List<Method> methods = new ArrayList<>(m);
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }
}
