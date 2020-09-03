package org.open.rules.project.validation.openapi.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public final class MethodUtils {

    private MethodUtils() {
    }

    private static final Comparator<IOpenMethod> METHOD_COMPARATOR = Comparator.comparing(IOpenMethod::getName)
        .thenComparingInt(e -> e.getSignature().getNumberOfParameters())
        .thenComparing(e -> e.getSignature().getParameterTypes(), MethodUtils::compareNames);

    private static int compareNames(IOpenClass[] p1, IOpenClass[] p2) {
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

    public static List<IOpenMethod> sort(Collection<IOpenMethod> m) {
        List<IOpenMethod> methods = new ArrayList<>(m);
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }

}
