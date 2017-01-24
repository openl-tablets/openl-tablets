package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;

public class DoubleRangeRulesParsingTest extends BaseOpenlBuilderHelper {
    
    private static final String SRC = "test/rules/helpers/DoubleRangeRulesParsingTest.xlsx";
    
    public DoubleRangeRulesParsingTest() {
        super(SRC);        
    }
    
    /**
     * checks that negative double ranges are being parsed from excel rule.
     */
    @Test    
    public void testDoubleNegativeRange() {
        assertEquals("rule1", invokeMethod("Hello1", -200.5));
        assertEquals("rule2", invokeMethod("Hello2", -60.5));
        assertEquals("rule3", invokeMethod("Hello3", -40.67));
    }
    
    private Object invokeMethod(String param1, double param2) {
        return invokeMethod("testDoubleNegativeRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), String.class), 
                OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), double.class)}, 
                new Object[]{param1, param2});
    }
    
    /**
     * Test that long can be processed through DoubleRange
     */
    @Test    
    public void testLongRange() {
        Object result = invokeMethod("testLongRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), double.class)}, 
                new Object[]{true, 210});
        assertEquals("rule1", result);
    }
    
    /**
     * Test that int can be processed through DoubleRange
     */
    @Test    
    public void testIntRange() {
        Object result = invokeMethod("testIntegerRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), double.class)}, 
                new Object[]{true, 105});
        assertEquals("rule2", result);
    }
    
    /**
     * Test that int can be processed through DoubleRange via DecisionTable with one condition(special case)
     */
    @Test    
    public void testIntRange1() {
        Object result = invokeMethod("testIntegerRange1", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), double.class)}, 
                new Object[]{true, 105});
        assertEquals("rule2", result);
    }
    
    /**
     * Test that byte can be processed through DoubleRange
     */
    @Test    
    public void testByteRange() {
        Object result = invokeMethod("testByteRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), double.class)}, 
                new Object[]{true, 99});
        assertEquals("rule2", result);
    }
    
    /**
     * Test that short can be processed through DoubleRange
     */
    @Test    
    public void testShortRange() {
        Object result = invokeMethod("testShortRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), double.class)}, 
                new Object[]{false, -50});
        assertEquals("rule3", result);
    }
    
    /**
     * Test that float can be processed through DoubleRange
     */
    @Test    
    public void testFloatRange() {
        Object result = invokeMethod("testFloatRange", 
            new IOpenClass[]{OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), boolean.class), 
                OpenClassHelper.getOpenClass(getCompiledOpenClass().getOpenClass(), double.class)}, 
                new Object[]{true, 20.56});
        assertEquals("rule1", result);
    }
    
    

}
