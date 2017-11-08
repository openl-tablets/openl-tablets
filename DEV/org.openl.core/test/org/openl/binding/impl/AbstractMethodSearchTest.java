package org.openl.binding.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeCastFactory;
import org.openl.types.IMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;

public abstract class AbstractMethodSearchTest {
    static final String AMB = "AMBIGUOUS";
    static final String NF = "NOT FOUND";
    static ICastFactory castFactory;
    private static class Not {
        Object notExpected;
    }

    @BeforeClass
    public static void init() {
        OpenLConfiguration openLConfiguration = new OpenLConfiguration();
        
        TypeCastFactory typecast = openLConfiguration.createTypeCastFactory();
        TypeCastFactory.JavaCastComponent javacast = typecast.new JavaCastComponent();
        javacast.setLibraryClassName(org.openl.binding.impl.cast.CastOperators.class.getName());
        javacast.setClassName(org.openl.binding.impl.cast.CastFactory.class.getName());
        typecast.addJavaCast(javacast);
        
        openLConfiguration.setConfigurationContext(new ConfigurableResourceContext(null));
        castFactory = openLConfiguration;
    }

    final void assertInvoke(Object expected, Class<?> target, String methodName, Class<?>... classes) {
        JavaOpenClass aClass = JavaOpenClass.getOpenClass(target);

        Object[] args = toArgs(classes);
        IMethodCaller method = MethodSearch.findMethod(methodName, JavaOpenClass.getOpenClasses(classes), castFactory, aClass);

        assertNotNull("Method " + methodDescriptor(methodName, classes) + " has not been matched", method);
        Object targetInstance = instance(target);
        Object result = method.invoke(targetInstance, args, null);
        assertEquals("Method " + methodDescriptor(methodName, classes) + " has been matched", expected, result);
    }

    final void assertNotFound(Class<?> target, String methodName, Class<?>... classes) {
        JavaOpenClass aClass = JavaOpenClass.getOpenClass(target);

        IMethodCaller method = MethodSearch
            .findMethod(methodName, JavaOpenClass.getOpenClasses(classes), castFactory, aClass);

        assertNull("Method " + methodDescriptor(methodName, classes) + " has been matched", method);
    }

    final void assertAmbigiouse(Class<?> target, String methodName, Class<?>... classes) {
        try {
            JavaOpenClass aClass = JavaOpenClass.getOpenClass(target);
            MethodSearch.findMethod(methodName, JavaOpenClass.getOpenClasses(classes), castFactory, aClass);
            fail("AmbiguousMethodException should be thrown for " + methodDescriptor(methodName, classes));
        } catch (AmbiguousMethodException ex) {
            // expected
        }
    }

    final void assertMethod(Class<?> target, String methodName, Class<?>[] classes, Object... expectes) {
        assertEquals(classes.length, expectes.length);
        for (int i = 0; i < classes.length; i++) {
            Object expected = expectes[i];
            assertMethod(expected, target, methodName, classes[i]);
        }
    }

    final void assertMethod(Class<?> target, String methodName, Class<?> class1, Class<?>[] classes, Object... expectes) {
        assertEquals(classes.length, expectes.length);
        for (int i = 0; i < classes.length; i++) {
            Object expected = expectes[i];
            assertMethod(expected, target, methodName, class1, classes[i]);
        }
    }

    final void assertMethod(Object expected, Class<?> target, String methodName, Class<?>... classes) {
        if (NF.equals(expected)) {
            assertNotFound(target, methodName, classes);
        } else if (AMB.equals(expected)) {
            assertAmbigiouse(target, methodName, classes);
        } else if (expected instanceof Not) {
            try {
                Object notExpected = ((Not) expected).notExpected;
                assertInvoke(notExpected, target, methodName, classes);
                fail("Not expected '" + notExpected +  "' result for Metod " + methodDescriptor(methodName, classes));
            } catch (AssertionError ex) {
                // It is expected an assertion error
            }
        } else {
            assertInvoke(expected, target, methodName, classes);
        }
    }

    final Object not (final Object expected) {
        return new Not() {{notExpected = expected;}};
    }

    private Object[] toArgs(Class<?>... classes) {
        Object[] args = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            args[i] = instance(classes[i]);
        }
        return args;
    }

    private Object instance(Class<?> clazz) {
        Object o;
        if (clazz.isPrimitive()) {
            clazz = ClassUtils.primitiveToWrapper(clazz);
        }
        try {
            o = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception exc) {
            try {
                o = clazz.getMethod("valueOf", String.class).invoke(null, "1");
            } catch (Exception exc2) {
                try {
                    o = clazz.getDeclaredConstructor(String.class).newInstance("2");
                } catch (Exception exc3) {
                    try {
                        o = clazz.getMethod("valueOf", char.class).invoke(null, 'A');
                    } catch (Exception exc4) {
                        o = null;
                    }
                }
            }
        }
        return o;
    }

    final ICastFactory getCastFactory() {
        return castFactory;
    }

    private String methodDescriptor(String name, Class<?>... args) {
        StringBuilder builder = new StringBuilder(100);
        builder.append(name).append('(');
        boolean flag = false;
        for (Class<?> arg : args) {
            if (flag) {
                builder.append(", ");
            }
            flag = true;
            builder.append(arg.getSimpleName());
        }
        builder.append(')');
        return builder.toString();
    }
}
