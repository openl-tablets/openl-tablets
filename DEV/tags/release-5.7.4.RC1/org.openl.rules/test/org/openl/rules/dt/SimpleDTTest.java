package org.openl.rules.dt;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * @author PUdalau
 */
public class SimpleDTTest extends BaseOpenlBuilderHelper {
    private static String __src = "./test/rules/dt/SimpleDTTest.xls";

    public SimpleDTTest() {
        super(__src);
    }

    @Test
    public void testLookup1D() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("simple",
                new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.STRING });
        Object wrapperInstance = getJavaWrapper().newInstance();
        assertEquals(new DoubleValue(0.02), method.invoke(wrapperInstance, new Object[] { 2, "v2" }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(0.05), method.invoke(wrapperInstance, new Object[] { 5, "v5" }, getJavaWrapper()
                .getEnv()));
    }

    @Test
    public void testLookup2D2params() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("simple2D2params",
                new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.STRING });
        Object wrapperInstance = getJavaWrapper().newInstance();
        assertEquals(new DoubleValue(0.01), method.invoke(wrapperInstance, new Object[] { 1, "v1" }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(0.09), method.invoke(wrapperInstance, new Object[] { 3, "v2" }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(0.17), method.invoke(wrapperInstance, new Object[] { 5, "v3" }, getJavaWrapper()
                .getEnv()));
    }

    @Test
    public void testLookup2D3params() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("simple2D3params",
                new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.STRING, JavaOpenClass.STRING });
        Object wrapperInstance = getJavaWrapper().newInstance();
        assertEquals(new DoubleValue(0.01), method.invoke(wrapperInstance, new Object[] { 1, "v1", "v1" },
                getJavaWrapper().getEnv()));
        assertEquals(new DoubleValue(0.08), method.invoke(wrapperInstance, new Object[] { 2, "v1", "v2" },
                getJavaWrapper().getEnv()));
        assertEquals(new DoubleValue(0.15), method.invoke(wrapperInstance, new Object[] { 3, "v2", "v1" },
                getJavaWrapper().getEnv()));
        assertEquals(new DoubleValue(0.22), method.invoke(wrapperInstance, new Object[] { 4, "v2", "v2" },
                getJavaWrapper().getEnv()));
    }
}
