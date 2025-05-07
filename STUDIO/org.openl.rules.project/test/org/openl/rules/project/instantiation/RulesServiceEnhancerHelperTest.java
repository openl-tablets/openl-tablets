package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.openl.rules.context.IRulesRuntimeContext;

public class RulesServiceEnhancerHelperTest {
    @Test
    public void testServiceClassDecoration() throws Exception {
        Class<?> enhanced = RuntimeContextInstantiationStrategyEnhancerHelper.decorateClass(SimpleInterface.class,
                Thread.currentThread().getContextClassLoader());
        checkEnhancement(enhanced, SimpleInterface.class, false);
    }

    private void checkEnhancement(Class<?> enhanced, Class<?> simple, boolean checkAnnotations) {
        assertEquals(enhanced.getMethods().length, simple.getMethods().length);
        // check methods
        for (Method method : simple.getMethods()) {
            try {
                Method m = enhanced.getMethod(method.getName(),
                        ArrayUtils.insert(0, method.getParameterTypes(), IRulesRuntimeContext.class));
                assertTrue(Modifier.isPublic(m.getModifiers()));
                if (checkAnnotations) {
                    // check annotations: all annotations should remain after
                    // undecoration.
                    // note: annotation passing to enhanced class currently is not
                    // supported.
                    assertArrayEquals(m.getAnnotations(), method.getAnnotations());
                }
            } catch (NoSuchMethodException e) {
                fail(e.getMessage());
            }
        }
        if (checkAnnotations) {
            // check annotations: all annotations should remain after
            // undecoration.
            // note: annotation passing to enhanced class currently is not
            // supported.
            assertArrayEquals(enhanced.getAnnotations(), simple.getAnnotations());
        }
    }

    @Test
    public void testServiceClassUndecoration() throws Exception {
        Class<?> undecorated = RuntimeContextInstantiationStrategyEnhancerHelper.undecorateClass(Enhanced.class,
                Thread.currentThread().getContextClassLoader());
        checkEnhancement(Enhanced.class, undecorated, true);
        Class<?> undecorated2 = RuntimeContextInstantiationStrategyEnhancerHelper.undecorateClass(Enhanced2.class,
                Thread.currentThread().getContextClassLoader());
        checkEnhancement(Enhanced2.class, undecorated2, true);
    }

    @Test
    public void testServiceClassRecognition() {
        assertTrue(RuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(Enhanced.class));
        assertTrue(RuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(Enhanced2.class));
        assertFalse(RuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(SimpleInterface.class));
        assertFalse(RuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(Mixed.class));
        assertFalse(RuntimeContextInstantiationStrategyEnhancerHelper.isDecoratedClass(Mixed2.class));
    }

    private interface Enhanced {
        void doSome(IRulesRuntimeContext context);

        void doSome2(IRulesRuntimeContext context, String arg);

        String getSome(IRulesRuntimeContext context, String arg, int arg2);
    }

    @XmlType
    private interface Enhanced2 {
        void doSome(IRulesRuntimeContext context);

        @Deprecated
        void doSome2(IRulesRuntimeContext context, String arg);

        @Disabled
        String getSome(IRulesRuntimeContext context, String arg, int arg2);
    }

    private interface SimpleInterface {
        void doSome();

        void doSome2(String arg);

        String getSome(String arg, int arg2);
    }

    private interface Mixed {
        void doSome();

        void doSome2(String arg);

        String getSome(IRulesRuntimeContext context, String arg, int arg2);
    }

    private interface Mixed2 {
        void doSome(IRulesRuntimeContext context);

        void doSome2(IRulesRuntimeContext context, String arg);

        String getSome(String arg, int arg2);
    }
}
