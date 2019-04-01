package org.openl.rules.project.instantiation;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import javax.xml.bind.annotation.XmlType;

import org.junit.Ignore;
import org.junit.Test;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancerHelper;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;

public class VariationsEnhancerHelperTest {

    private void checkEnhancement(Class<?> enhanced, Class<?> simple, boolean checkAnnotations) throws Exception {
        // check methods
        for (Method method : enhanced.getMethods()) {
            assertNotNull(VariationInstantiationStrategyEnhancerHelper.getMethodForDecoration(simple, method));
        }
        if (checkAnnotations) {
            // check annotations: all annotations should remain after
            // undecoration.
            // note: annotation passing to enhanced class currently is not
            // supported.
            assertArrayEquals(enhanced.getAnnotations(), simple.getAnnotations());
            for (Method method : simple.getMethods()) {
                if (!VariationInstantiationStrategyEnhancerHelper.isDecoratedMethod(method)) {
                    assertArrayEquals(
                        VariationInstantiationStrategyEnhancerHelper.getMethodForDecoration(simple, method)
                            .getAnnotations(),
                        method.getAnnotations());
                }
            }
        }
    }

    @Test
    public void testServiceClassDecoration() throws Exception {
        Class<?> enhanced = VariationInstantiationStrategyEnhancerHelper.decorateClass(SimpleInterface.class,
            Thread.currentThread().getContextClassLoader());
        checkEnhancement(enhanced, SimpleInterface.class, false);
        Class<?> enhanced2 = VariationInstantiationStrategyEnhancerHelper.decorateClass(SimpleInterface2.class,
            Thread.currentThread().getContextClassLoader());
        checkEnhancement(enhanced2, SimpleInterface2.class, false);
        checkEnhancement(InterfaceFullyEnhanced.class, SimpleInterface.class, true);
        checkEnhancement(InterfacePartiallyEnhanced.class, SimpleInterface.class, true);
        checkEnhancement(Interface2FullyEnhanced.class, SimpleInterface2.class, true);
        checkEnhancement(Interface2PartiallyEnhanced.class, SimpleInterface2.class, true);
    }

    @Test
    public void testServiceClassUndecoration() throws Exception {
        Class<?> undecorateFully1 = VariationInstantiationStrategyEnhancerHelper
            .undecorateClass(InterfaceFullyEnhanced.class, Thread.currentThread().getContextClassLoader());
        checkEnhancement(InterfaceFullyEnhanced.class, undecorateFully1, true);
        Class<?> undecoratePartially1 = VariationInstantiationStrategyEnhancerHelper
            .undecorateClass(InterfacePartiallyEnhanced.class, Thread.currentThread().getContextClassLoader());
        checkEnhancement(InterfacePartiallyEnhanced.class, undecoratePartially1, true);
        Class<?> undecorateFully2 = VariationInstantiationStrategyEnhancerHelper
            .undecorateClass(Interface2FullyEnhanced.class, Thread.currentThread().getContextClassLoader());
        checkEnhancement(Interface2FullyEnhanced.class, undecorateFully2, true);
        Class<?> undecoratePartially2 = VariationInstantiationStrategyEnhancerHelper
            .undecorateClass(Interface2PartiallyEnhanced.class, Thread.currentThread().getContextClassLoader());
        checkEnhancement(Interface2PartiallyEnhanced.class, undecoratePartially2, true);
    }

    @Test
    public void testVariationsRecognition() {
        assertTrue(VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(InterfaceFullyEnhanced.class));
        assertTrue(VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(InterfacePartiallyEnhanced.class));
        assertTrue(VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(Interface2FullyEnhanced.class));
        assertTrue(VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(Interface2PartiallyEnhanced.class));
        assertFalse(VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(SimpleInterface.class));
        assertFalse(VariationInstantiationStrategyEnhancerHelper.isDecoratedClass(SimpleInterface2.class));
    }

    private interface SimpleInterface {
        String doSome();

        String doSome2(String arg);

        String getSome(String arg, int arg2);
    }

    private interface InterfaceFullyEnhanced {
        String doSome();

        VariationsResult<String> doSome(VariationsPack variations);

        String doSome2(String arg);

        VariationsResult<String> doSome2(String arg, VariationsPack variations);

        String getSome(String arg, int arg2);

        VariationsResult<String> getSome(String arg, int arg2, VariationsPack variations);
    }

    private interface InterfacePartiallyEnhanced {
        String doSome();

        String doSome2(String arg);

        String getSome(String arg, int arg2);

        VariationsResult<String> getSome(String arg, int arg2, VariationsPack variations);
    }

    @XmlType
    private interface SimpleInterface2 {
        double doSome();

        @Deprecated
        double doSome2(double arg);

        @Ignore
        double getSome(String arg, double arg2);
    }

    @XmlType
    private interface Interface2FullyEnhanced {
        Double doSome();

        VariationsResult<Double> doSome(VariationsPack variations);

        @Deprecated
        Double doSome2(double arg);

        VariationsResult<Double> doSome2(double arg, VariationsPack variations);

        @Ignore
        Double getSome(String arg, double arg2);

        VariationsResult<Double> getSome(String arg, double arg2, VariationsPack variations);
    }

    @XmlType
    private interface Interface2PartiallyEnhanced {
        Double doSome();

        VariationsResult<Double> doSome(VariationsPack variations);

        @Deprecated
        Double doSome2(double arg);

        VariationsResult<Double> doSome2(double arg, VariationsPack variations);

        @Ignore
        Double getSome(String arg, double arg2);

        VariationsResult<Double> getSome(String arg, double arg2, VariationsPack variations);
    }

}
