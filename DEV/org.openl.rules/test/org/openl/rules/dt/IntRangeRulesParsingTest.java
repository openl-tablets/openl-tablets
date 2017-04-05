package org.openl.rules.dt;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.type.IntRangeAdaptor;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * 
 * @author DLiauchuk
 *
 */
public class IntRangeRulesParsingTest extends BaseOpenlBuilderHelper {
    
    private static final String SRC = "test/rules/helpers/IntRangeTest.xlsx";
    
    public IntRangeRulesParsingTest() {
        super(SRC);        
    }
    
    /**
     * Test to check EPBDS-2128 issue.
     */
    @Test    
    public void test() {
        assertEquals("rule2", invoke("getLossAssessment", true, 9999));
    }
    
    /**
     * Test that negative int ranges are supported.
     */
    @Test    
    public void testNegativeRange() {        
        assertEquals("rule1", invoke("testNegativeRange", true, -205));
        
        assertEquals("rule2", invoke("testNegativeRange", true, -103));        
        
        assertEquals("rule3", invoke("testNegativeRange", false, -80));
        
        assertEquals("rule3", invoke("testNegativeRange", false, -100));
        
        assertEquals("rule4", invoke("testNegativeRange", true, -20));
                 
        assertEquals("rule5", invoke("testNegativeRange", false, -20));
    }
    
    /**
     * Test that byte value can be processed through IntRange 
     */
    @Test    
    public void testByteRange() {
        assertEquals("rule2", invoke("testByteRange", true, 110));
    }
    
    /**
     * Test that short value can be processed through IntRange
     */
    @Test    
    public void testShortRange() {
        assertEquals("rule1", invoke("testShortRange", true, 202));
    }
    
    /**
     * Test that Integer.MAX_VALUE won`t get to range. As during current implementation it can`t be covered. 
     * See {@link IntRangeAdaptor#getMax(org.openl.rules.helpers.IntRange)} 
     */
    @Test    
    public void testMaxInt() {
        assertNull(invoke("testMaxInt", true, 2147483646));
    }
    
    @Test    
    public void testMaxInt1() {
        assertEquals("rule1", invoke("testMaxInt1", true, 2147483645));
    }
    
    @Test    
    public void testtestRange() {
        assertEquals("rule1", invokeMethod("ClassifyIncome", 
            new IOpenClass[]{JavaOpenClass.STRING, JavaOpenClass.SHORT},
                new Object[]{"Type 1", -300}));
    }
    
    @Test    
    public void testtestRange0() {
        assertEquals("rule3", invokeMethod("ClassifyIncome", 
            new IOpenClass[]{JavaOpenClass.STRING, JavaOpenClass.SHORT},
                new Object[]{"Type 2", -80}));
    }
    
    private Object invoke(String methodName, boolean param1, int param2) {
        return invokeMethod(methodName, 
            new IOpenClass[]{JavaOpenClass.BOOLEAN, JavaOpenClass.INT},
              new Object[]{param1, param2});
    }

}
