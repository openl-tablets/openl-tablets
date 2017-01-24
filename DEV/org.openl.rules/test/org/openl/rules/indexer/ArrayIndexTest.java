package org.openl.rules.indexer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class ArrayIndexTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "./test/rules/ArrayIndexTest.xlsx";

    public ArrayIndexTest() {
        super(SRC);
    }

    @Test
    public void testArray1() {//ShortValue
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork1",
                new IOpenClass[] { });
        Object instance = newInstance();
        
       assertEquals(new org.openl.meta.ShortValue((Short.valueOf("-10").shortValue())), method.invoke(instance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }
    
    @Test
    public void testArray2() {//IntValue
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork2",
                new IOpenClass[] { });
        Object wrapperInstance = newInstance();
        
       assertEquals(new org.openl.meta.IntValue((Integer.valueOf("-10").intValue())), method.invoke(wrapperInstance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }
    
    @Test
    public void testArray3() {//int
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork3",
                new IOpenClass[] { });
        Object wrapperInstance = newInstance();
        
       assertEquals(-10, method.invoke(wrapperInstance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }
    
    @Test
    public void testArray4() {//long
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork4",
                new IOpenClass[] { });
        Object wrapperInstance = newInstance();
        
       assertEquals(-10L, method.invoke(wrapperInstance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }

    @Test
    public void testArray5() {//Long
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork5",
                new IOpenClass[] { });
        Object wrapperInstance = newInstance();
        
       assertEquals(Long.valueOf("-10"), method.invoke(wrapperInstance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }

    @Test
    public void testArray6() {//Byte
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork6",
                new IOpenClass[] { });
        Object wrapperInstance = newInstance();
        
       assertEquals(Byte.valueOf("-10"), method.invoke(wrapperInstance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }

    @Test
    public void testArray7() {//byte
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork7",
                new IOpenClass[] { });
        Object wrapperInstance = newInstance();
        
       assertEquals(Byte.valueOf("-10").byteValue(), method.invoke(wrapperInstance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }

    @Test
    public void testArray8() {//Integer
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("isWork8",
                new IOpenClass[] { });
        Object wrapperInstance = newInstance();
        
       assertEquals(Integer.valueOf("-10"), method.invoke(wrapperInstance, new Object[] { }, new SimpleRulesVM().getRuntimeEnv()));
    }
}