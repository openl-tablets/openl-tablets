package org.openl.rules.ruleservice.publish.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MethodSorter {
    private MethodSorter() {
    }

    public static List<Method> sort(Collection<Method> m) {
        List<Method> methods = new ArrayList<Method>(m);
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                if (o1.getName().equals(o2.getName())) {
                    if (o1.getParameterTypes().length == o2.getParameterTypes().length) {
                        int i = 0;
                        while (i < o1.getParameterTypes().length && o1.getParameterTypes()[i].equals(o2.getParameterTypes()[i])) {
                            i++;
                        }
                        return o1.getParameterTypes()[i].getName().compareTo(o2.getParameterTypes()[i].getName());
                    } else {
                        return o1.getParameterTypes().length - o2.getParameterTypes().length;
                    }
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            };
        });
        return methods;
    }
}
