package org.openl.types.impl;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class AOpenClassTest {

    @Test
    public void superclass_methods_mustBePresent_whenInheritorDoesNotHaveMethods() {
        AOpenClass openClass = JavaOpenClass.getOpenClass(B.class);
        Collection<IOpenMethod> methods = openClass.getMethods();
        assertNotNull(methods);
        assertFalse(methods.isEmpty());
        assertNotNull(findMethod(methods, "foo"));
        assertNotNull(findMethod(methods, "bar"));

        openClass = JavaOpenClass.getOpenClass(C.class);
        methods = openClass.getMethods();
        assertNotNull(methods);
        assertFalse(methods.isEmpty());
        assertNotNull(findMethod(methods, "foo"));
        assertNotNull(findMethod(methods, "bar"));
        assertNull(findMethod(methods, "getC"));
    }

    private static IOpenMethod findMethod(Collection<IOpenMethod> methods, String name) {
        for (IOpenMethod method : methods) {
            if (name.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    private static class A {

        public void foo() {
        }

        public void bar() {
        }

    }

    public static class B extends A {
    }

    public static class C extends A {

        private void getC() {
        }

    }

}
