package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;

public class DoubleRangeRulesParsingTest extends BaseOpenlBuilderHelper {
    
    private static String __src = "test/rules/helpers/DoubleRangeRulesParsingTest.xlsx";
    
    public DoubleRangeRulesParsingTest() {
        super(__src);        
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
            new IOpenClass[]{OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), String.class), 
                OpenClassHelper.getOpenClass(getJavaWrapper().getCompiledClass().getOpenClass(), double.class)}, 
                new Object[]{param1, param2});
    }

}
