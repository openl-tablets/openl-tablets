package org.openl.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public final class OpenClassUtils {

    private OpenClassUtils() {
    }

    public static IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
        IOpenClass t1 = openClass1;
        IOpenClass t2 = openClass2;
        if (t1.getInstanceClass() != null && t2.getInstanceClass() != null) {
            if (t1.getInstanceClass().isPrimitive() && !t2.getInstanceClass().isPrimitive() || !t1.getInstanceClass()
                .isPrimitive() && t2.getInstanceClass().isPrimitive()) {
                if (t1.getInstanceClass().isPrimitive()) {
                    t1 = JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(t1.getInstanceClass()));
                }
                if (t2.getInstanceClass().isPrimitive()) {
                    t2 = JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(t2.getInstanceClass()));
                }
            }
        }
        return findParentClassNoPrimitives(t1, t2);
    }

    private static IOpenClass findParentClassNoPrimitives(IOpenClass class1, IOpenClass class2) {
        if (NullOpenClass.isAnyNull(class1)) {
            if (NullOpenClass.isAnyNull(class2)) {
                return class2;
            }
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(class2.getInstanceClass()));
        }
        if (NullOpenClass.isAnyNull(class2)) {
            if (NullOpenClass.isAnyNull(class1)) {
                return class1;
            }
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(class1.getInstanceClass()));
        }

        if (class1.isArray() && class2.isArray()) {
            int dim = 0;
            while (class1.isArray() && class2.isArray()) {
                dim++;
                class1 = class1.getComponentClass();
                class2 = class2.getComponentClass();
            }
            IOpenClass parentClass = findParentClassNoPrimitives(class1, class2);
            if (parentClass == null) {
                return null;
            }
            return parentClass.getArrayType(dim);
        }

        if (class1.getInstanceClass() == null && class2.getInstanceClass() == null) {
            return class1;
        }

        // If class1 is NULL literal
        if (class1.getInstanceClass() == null) {
            if (class2.getInstanceClass().isPrimitive()) {
                return null;
            } else {
                return class2;
            }
        }

        // If class2 is NULL literal
        if (class2.getInstanceClass() == null) {
            if (class1.getInstanceClass().isPrimitive()) {
                return null;
            } else {
                return class1;
            }
        }

        if (class1.getInstanceClass().isPrimitive() || class2.getInstanceClass().isPrimitive()) { // If
            // one
            // is
            // primitive
            if (class1.equals(class2)) {
                return class1;
            }
            return null;
        }
        Set<IOpenClass> superClasses = new HashSet<>();
        superClasses.add(class1);
        IOpenClass openClass = class1;
        Set<IOpenClass> interfaces = new LinkedHashSet<>();
        while (openClass != null && !JavaOpenClass.OBJECT.equals(openClass)) {
            IOpenClass next = null;
            for (IOpenClass x : openClass.superClasses()) {
                if (!x.isInterface()) {
                    superClasses.add(x);
                    next = x;
                } else {
                    interfaces.add(x);
                }
            }
            openClass = next;
        }
        if (superClasses.contains(class2)) {
            return class2;
        }
        openClass = class2;
        while (openClass != null && !JavaOpenClass.OBJECT.equals(openClass)) {
            IOpenClass next = null;
            for (IOpenClass x : openClass.superClasses()) {
                if (!x.isInterface()) {
                    if (superClasses.contains(x)) {
                        return x;
                    }
                    next = x;
                }
            }
            openClass = next;
        }
        Queue<IOpenClass> queue = new ArrayDeque<>(interfaces);
        while (!queue.isEmpty()) {
            Set<IOpenClass> queue1 = new LinkedHashSet<>();
            for (IOpenClass oc : queue) {
                StreamSupport.stream(oc.superClasses().spliterator(), false)
                    .filter(IOpenClass::isInterface)
                    .filter(e -> !interfaces.contains(e))
                    .forEach(e -> {
                        interfaces.add(e);
                        queue1.add(e);
                    });
            }
            queue = new ArrayDeque<>(queue1);
        }
        queue = new ArrayDeque<>();
        for (IOpenClass x : class2.superClasses()) {
            if (x.isInterface()) {
                queue.add(x);
            }
        }
        while (!queue.isEmpty()) {
            Set<IOpenClass> queue1 = new LinkedHashSet<>();
            for (IOpenClass oc : queue) {
                if (oc.getInstanceClass().getTypeParameters().length == 0 && interfaces.contains(oc)) {
                    return oc;
                }
                StreamSupport.stream(oc.superClasses().spliterator(), false)
                    .filter(IOpenClass::isInterface)
                    .filter(e -> !interfaces.contains(e))
                    .forEach(queue1::add);
            }
            queue = new ArrayDeque<>(queue1);
        }
        return JavaOpenClass.OBJECT;
    }

    public static IOpenClass getRootComponentClass(IOpenClass fieldType) {
        if (!fieldType.isArray()) {
            return fieldType;
        }
        // Get the component type of the array
        //
        return getRootComponentClass(fieldType.getComponentClass());
    }

    public static boolean isVoid(IOpenClass type) {
        return JavaOpenClass.VOID.equals(type) || JavaOpenClass.CLS_VOID.equals(type);
    }
}
