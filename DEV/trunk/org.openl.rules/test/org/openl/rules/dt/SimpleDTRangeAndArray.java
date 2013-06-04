package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class SimpleDTRangeAndArray extends BaseOpenlBuilderHelper {
    private static final String SRC = "./test/rules/dt/SimpleDTRangeAndArray.xlsx";

    public SimpleDTRangeAndArray() {
        super(SRC);
    }
    
    @Test
    public void testArray1() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("TestArray1",
                new IOpenClass[] { JavaOpenClass.STRING });
        Object wrapperInstance = getJavaWrapper().newInstance();
        
       assertEquals(0, method.invoke(wrapperInstance, new Object[] { "1234" }, getJavaWrapper()
                .getEnv()));
        assertEquals(5, method.invoke(wrapperInstance, new Object[] { "-3" }, getJavaWrapper()
                .getEnv()));
        assertEquals(0, method.invoke(wrapperInstance, new Object[] { "erty" }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void testArray2() {
        IOpenMethod method = getMethod("TestArray2",
                    new IOpenClass[] { JavaOpenClass.STRING });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(1, method.invoke(wrapperInstance, new Object[] { "1234" }, getJavaWrapper()
                .getEnv()));
        assertEquals(12, method.invoke(wrapperInstance, new Object[] { "werwe" }, getJavaWrapper()
                .getEnv()));
        assertEquals(5, method.invoke(wrapperInstance, new Object[] { "asda" }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void testRangeInt() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("TestRangeInt",
                    new IOpenClass[] { JavaOpenClass.INT });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(-10, method.invoke(wrapperInstance, new Object[] { 67 }, getJavaWrapper()
                .getEnv()));
        assertEquals(89, method.invoke(wrapperInstance, new Object[] { 99 }, getJavaWrapper()
                .getEnv()));
        assertEquals(78, method.invoke(wrapperInstance, new Object[] { 3 }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void testRangeDouble1() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("TestRangeDouble1",
                    new IOpenClass[] { JavaOpenClass.DOUBLE });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(-10, method.invoke(wrapperInstance, new Object[] { 150.005d }, getJavaWrapper()
                .getEnv()));
        assertEquals(85, method.invoke(wrapperInstance, new Object[] { 0d }, getJavaWrapper()
                .getEnv()));
        assertEquals(78, method.invoke(wrapperInstance, new Object[] { 6000000d }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void simpleLookup2Range() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("SimpleLookup2Range",
                    new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class) });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(new DoubleValue(0), method.invoke(wrapperInstance, new Object[] { "DE", 150.005d }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(1), method.invoke(wrapperInstance, new Object[] { "", 150.005d }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(1), method.invoke(wrapperInstance, new Object[] { "DE", 2000d }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void simpleLookupRange1() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("SimpleLookupRange1",
                    new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class) });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(new DoubleValue(0.9), method.invoke(wrapperInstance, new Object[] { "DE", 25 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(1), method.invoke(wrapperInstance, new Object[] { "", 4 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(0), method.invoke(wrapperInstance, new Object[] { "DE", 3 }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void simpleLookup3paramRangeArray() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("SimpleLookup3paramRangeArray",
                    new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class), JavaOpenClass.INT });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(new DoubleValue(0), method.invoke(wrapperInstance, new Object[] { "DE", 5d, 5 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(0.9), method.invoke(wrapperInstance, new Object[] { "DE", 7d, 3 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(1), method.invoke(wrapperInstance, new Object[] { "DE", 10d, 4 }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void simpleLookup4paramTitleRange() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("SimpleLookup4paramTitleRange",
                    new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class),
                        JavaOpenClass.INT, JavaOpenClass.INT });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(new DoubleValue(0.9), method.invoke(wrapperInstance, new Object[] { "DE", 0d, 7, 3 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(1), method.invoke(wrapperInstance, new Object[] { "DE", 0d, 10, 4 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(56), method.invoke(wrapperInstance, new Object[] { "", 1d, 9, 3 }, getJavaWrapper()
                .getEnv()));
    }
    
    @Test
    public void simpleLookup4paramNotEnoughValues() {
        IOpenMethod method = getJavaWrapper().getOpenClass().getMethod("SimpleLookup4paramNotEnoughValues",
                    new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.getOpenClass(Double.class),
                        JavaOpenClass.INT, JavaOpenClass.INT });
        Object wrapperInstance = getJavaWrapper().newInstance();
            
        assertEquals(new DoubleValue(0.9), method.invoke(wrapperInstance, new Object[] { "DE", 0d, 7, 3 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(1), method.invoke(wrapperInstance, new Object[] { "DE", 0d, 10, 4 }, getJavaWrapper()
                .getEnv()));
        assertEquals(new DoubleValue(56), method.invoke(wrapperInstance, new Object[] { "", 1d, 9, 3 }, getJavaWrapper()
                .getEnv()));
    }
}
