package org.openl.rules.lang.xls.types.meta;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openl.binding.MethodUtil;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class MetaInfoUtils {
    private static final Set<Class<?>> INTERFACES_AS_OBJECT = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(Serializable.class, Cloneable.class)));

    public static String openClassToDisplayName(IOpenClass type) {
        IOpenClass t = type;
        StringBuilder sb = new StringBuilder();
        while (t.isArray()) {
            t = t.getComponentClass();
            sb.append("[]");
        }
        if (INTERFACES_AS_OBJECT.contains(t.getInstanceClass())) {
            return MethodUtil.printType(JavaOpenClass.OBJECT) + sb.toString() + "(" + MethodUtil.printType(t) + sb
                .toString() + ")";
        } else {
            return MethodUtil.printType(type);
        }
    }
}
