package org.openl.rules.dt;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;


public class IntRangeRulesParsingTest extends BaseOpenlBuilderHelper {
    
    private static String __src = "test/rules/helpers/IntRangeTest.xlsx";
    
    public IntRangeRulesParsingTest() {
        super(__src);        
    }
    
    /**
     * Test to check EPBDS-2128 issue.
     */
    @Test    
    public void test() {
        Object result = invokeMethod("getLossAssessment", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), int.class)}, 
                new Object[]{true, 9999});
        assertEquals("rule2", result);
    }
    
    /**
     * Test that negative int ranges are supported.
     */
    @Test    
    public void testNegativeRange() {        
        assertEquals("rule1", invokeNegativeRangeFunc(true, -205));
        
        assertEquals("rule2", invokeNegativeRangeFunc(true, -103));        
        
        assertEquals("rule3", invokeNegativeRangeFunc(false, -80));
        
        assertEquals("rule3", invokeNegativeRangeFunc(false, -100));
        
        assertEquals("rule4", invokeNegativeRangeFunc(true, -20));
                 
        assertEquals("rule5", invokeNegativeRangeFunc(false, -20));
    }
        
    private Object invokeNegativeRangeFunc(boolean param1, int param2) {
        return invokeMethod("testNegativeRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), int.class)}, 
                new Object[]{param1, param2});
    }
    
    /**
     * Test that byte value can be processed through IntRange 
     */
    @Test    
    public void testByteRange() {
        Object result = invokeMethod("testByteRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), int.class)}, 
                new Object[]{true, 110});
        assertEquals("rule2", result);
    }
    
    /**
     * Test that short value can be processed through IntRange
     */
    @Test    
    public void testShortRange() {
        Object result = invokeMethod("testShortRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), int.class)}, 
                new Object[]{true, 202});
        assertEquals("rule1", result);
    }

}
