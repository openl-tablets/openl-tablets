package org.openl.rules.dt;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;


public class IntRangeTest extends BaseOpenlBuilderHelper {
    
    private static String __src = "test/rules/dt/IntRangeTest.xlsx";
    
    public IntRangeTest() {
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

}
