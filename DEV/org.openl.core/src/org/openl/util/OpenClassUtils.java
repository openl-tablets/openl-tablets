package org.openl.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ComponentTypeArrayOpenClass;
import org.openl.types.java.JavaOpenClass;

public final class OpenClassUtils {

    private OpenClassUtils() {
    }

    public static IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
        if (openClass1 == null) {
            throw new IllegalArgumentException("openClass1 cannot be null");
        }
        if (openClass2 == null) {
            throw new IllegalArgumentException("openClass2 cannot be null");
        }
        if (NullOpenClass.isAnyNull(openClass1)) {
            if (NullOpenClass.isAnyNull(openClass2)) {
                return openClass2;
            }
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass2.getInstanceClass()));
        }
        if (NullOpenClass.isAnyNull(openClass2)) {
            if (NullOpenClass.isAnyNull(openClass1)) {
                return openClass1;
            }
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass1.getInstanceClass()));
        }

        if (openClass1.getInstanceClass() != null && openClass2.getInstanceClass() != null) {
            if (openClass1.getInstanceClass().isPrimitive() && !openClass2.getInstanceClass()
                .isPrimitive() || !openClass1.getInstanceClass().isPrimitive() && openClass2.getInstanceClass()
                    .isPrimitive()) {
                if (openClass1.getInstanceClass().isPrimitive()) {
                    openClass1 = JavaOpenClass
                        .getOpenClass(ClassUtils.primitiveToWrapper(openClass1.getInstanceClass()));
                }
                if (openClass2.getInstanceClass().isPrimitive()) {
                    openClass2 = JavaOpenClass
                        .getOpenClass(ClassUtils.primitiveToWrapper(openClass2.getInstanceClass()));
                }
            }
        }

        if (openClass1.isArray() && openClass2.isArray()) {
            int dim = 0;
            while (openClass1.isArray() && openClass2.isArray()) {
                openClass1 = openClass1.getComponentClass();
                openClass2 = openClass2.getComponentClass();
                dim++;
            }
            IOpenClass parentClass = findParentClass(openClass1, openClass2);
            if (parentClass == null) {
                return null;
            }
            return ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(parentClass, dim);
        }

        if (openClass1.getInstanceClass() == null && openClass2.getInstanceClass() == null) {
            return openClass1;
        }

        // If class1 is NULL literal
        if (openClass1.getInstanceClass() == null) {
            if (openClass2.getInstanceClass().isPrimitive()) {
                return null;
            } else {
                return openClass2;
            }
        }

        // If class2 is NULL literal
        if (openClass2.getInstanceClass() == null) {
            if (openClass1.getInstanceClass().isPrimitive()) {
                return null;
            } else {
                return openClass1;
            }
        }

        if (openClass1.getInstanceClass().isPrimitive() || openClass2.getInstanceClass().isPrimitive()) { // If
            // one
            // is
            // primitive
            if (openClass1.equals(openClass2)) {
                return openClass1;
            }
            return null;
        }
        Set<IOpenClass> superClasses = new HashSet<>();
        superClasses.add(openClass1);
        if (!openClass1.equals(JavaOpenClass.getOpenClass(openClass1.getInstanceClass()))) {
            superClasses.add(JavaOpenClass.getOpenClass(openClass1.getInstanceClass()));
        }
        IOpenClass openClass = openClass1;
        Set<IOpenClass> interfaces = new LinkedHashSet<>();
        if (openClass.isInterface()) {
            interfaces.add(openClass);
        }
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
        if (superClasses.contains(openClass2)) {
            return openClass2;
        }
        if (superClasses.contains(JavaOpenClass.getOpenClass(openClass2.getInstanceClass()))) {
            return JavaOpenClass.getOpenClass(openClass2.getInstanceClass());
        }
        openClass = openClass2;
        while (openClass != null && !JavaOpenClass.OBJECT.equals(openClass)) {
            IOpenClass next = null;
            for (IOpenClass x : openClass.superClasses()) {
                if (!x.isInterface()) {
                    if (!JavaOpenClass.OBJECT.equals(x) && superClasses.contains(x)) {
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
                oc.superClasses()
                    .stream()
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
        if (openClass2.isInterface()) {
            queue.add(openClass2);
        }
        openClass2.superClasses().stream().filter(IOpenClass::isInterface).forEach(queue::add);
        while (!queue.isEmpty()) {
            Set<IOpenClass> queue1 = new LinkedHashSet<>();
            for (IOpenClass oc : queue) {
                if (oc.getInstanceClass().getTypeParameters().length == 0 && interfaces.contains(oc)) {
                    return oc;
                }
                oc.superClasses()
                    .stream()
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
