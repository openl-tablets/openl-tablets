package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

/**
 * @author PUdalau
 */
public class SimpleDTTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "./test/rules/dt/SimpleDTTest.xls";

    public SimpleDTTest() {
        super(SRC);
    }

    @Test
    public void testLookup1D() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("simple",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.STRING });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0.02),
            method.invoke(instance, new Object[] { 2, "v2" }, env));
        assertEquals(new DoubleValue(0.05),
            method.invoke(instance, new Object[] { 5, "v5" }, env));
    }

    @Test
    public void testLookup2D2params() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("simple2D2params",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.STRING });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0.01),
            method.invoke(instance, new Object[] { 1, "v1" }, env));
        assertEquals(new DoubleValue(0.09),
            method.invoke(instance, new Object[] { 3, "v2" }, env));
        assertEquals(new DoubleValue(0.17),
            method.invoke(instance, new Object[] { 5, "v3" }, env));
    }

    @Test
    public void testLookup2D3params() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("simple2D3params",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.STRING, JavaOpenClass.STRING });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0.01),
            method.invoke(instance, new Object[] { 1, "v1", "v1" }, env));
        assertEquals(new DoubleValue(0.08),
            method.invoke(instance, new Object[] { 2, "v1", "v2" }, env));
        assertEquals(new DoubleValue(0.15),
            method.invoke(instance, new Object[] { 3, "v2", "v1" }, env));
        assertEquals(new DoubleValue(0.22),
            method.invoke(instance, new Object[] { 4, "v2", "v2" }, env));
    }
}
