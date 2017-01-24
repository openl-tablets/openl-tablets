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

public class SimpleDTRangeAndArray extends BaseOpenlBuilderHelper {
    private static final String SRC = "./test/rules/dt/SimpleDTRangeAndArray.xlsx";

    public SimpleDTRangeAndArray() {
        super(SRC);
    }

    @Test
    public void testArray1() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("TestArray1",
            new IOpenClass[] { JavaOpenClass.STRING });
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object instance = newInstance();

        assertEquals(0, method.invoke(instance, new Object[] { "1234" }, env));
        assertEquals(5, method.invoke(instance, new Object[] { "-3" }, env));
        assertEquals(0, method.invoke(instance, new Object[] { "erty" }, env));
    }

    @Test
    public void testArray2() {
        IOpenMethod method = getMethod("TestArray2", new IOpenClass[] { JavaOpenClass.STRING });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(1, method.invoke(instance, new Object[] { "1234" }, env));
        assertEquals(12, method.invoke(instance, new Object[] { "werwe" }, env));
        assertEquals(5, method.invoke(instance, new Object[] { "asda" }, env));
    }

    @Test
    public void testRangeInt() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("TestRangeInt",
            new IOpenClass[] { JavaOpenClass.INT });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(-10, method.invoke(instance, new Object[] { 67 }, env));
        assertEquals(89, method.invoke(instance, new Object[] { 99 }, env));
        assertEquals(78, method.invoke(instance, new Object[] { 3 }, env));
    }

    @Test
    public void testRangeDouble1() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("TestRangeDouble1",
            new IOpenClass[] { JavaOpenClass.DOUBLE });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(-10, method.invoke(instance, new Object[] { 150.005d }, env));
        assertEquals(85, method.invoke(instance, new Object[] { 0d }, env));
        assertEquals(78, method.invoke(instance, new Object[] { 6000000d }, env));
    }

    @Test
    public void simpleLookup2Range() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("SimpleLookup2Range",
            new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0),
            method.invoke(instance, new Object[] { "DE", 150.005d }, env));
        assertEquals(new DoubleValue(1),
            method.invoke(instance, new Object[] { "", 150.005d }, env));
        assertEquals(new DoubleValue(1),
            method.invoke(instance, new Object[] { "DE", 2000d }, env));
    }

    @Test
    public void simpleLookupRange1() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("SimpleLookupRange1",
            new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0.9),
            method.invoke(instance, new Object[] { "DE", 25 }, env));
        assertEquals(new DoubleValue(1), method.invoke(instance, new Object[] { "", 4 }, env));
        assertEquals(new DoubleValue(0), method.invoke(instance, new Object[] { "DE", 3 }, env));
    }

    @Test
    public void simpleLookup3paramRangeArray() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("SimpleLookup3paramRangeArray",
            new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class), JavaOpenClass.INT });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0),
            method.invoke(instance, new Object[] { "DE", 5d, 5 }, env));
        assertEquals(new DoubleValue(0.9),
            method.invoke(instance, new Object[] { "DE", 7d, 3 }, env));
        assertEquals(new DoubleValue(1),
            method.invoke(instance, new Object[] { "DE", 10d, 4 }, env));
    }

    @Test
    public void simpleLookup4paramTitleRange() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("SimpleLookup4paramTitleRange",
            new IOpenClass[] { JavaOpenClass.STRING,
                    JavaOpenClass.getOpenClass(Double.class),
                    JavaOpenClass.INT,
                    JavaOpenClass.INT });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0.9),
            method.invoke(instance, new Object[] { "DE", 0d, 7, 3 }, env));
        assertEquals(new DoubleValue(1),
            method.invoke(instance, new Object[] { "DE", 0d, 10, 4 }, env));
        assertEquals(new DoubleValue(56),
            method.invoke(instance, new Object[] { "", 1d, 9, 3 }, env));
    }

    @Test
    public void simpleLookup4paramNotEnoughValues() {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("SimpleLookup4paramNotEnoughValues",
            new IOpenClass[] { JavaOpenClass.STRING,
                    JavaOpenClass.getOpenClass(Double.class),
                    JavaOpenClass.INT,
                    JavaOpenClass.INT });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        assertEquals(new DoubleValue(0.9),
            method.invoke(instance, new Object[] { "DE", 0d, 7, 3 }, env));
        assertEquals(new DoubleValue(1),
            method.invoke(instance, new Object[] { "DE", 0d, 10, 4 }, env));
        assertEquals(new DoubleValue(56),
            method.invoke(instance, new Object[] { "", 1d, 9, 3 }, env));
    }
}
