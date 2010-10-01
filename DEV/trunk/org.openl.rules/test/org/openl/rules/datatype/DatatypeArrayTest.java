package org.openl.rules.datatype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.OpenClassHelper;

public class DatatypeArrayTest extends BaseOpenlBuilderHelper {
    
    private static final String src = "test/rules/datatype/DatatypeArray.xls";
    
    public DatatypeArrayTest() {
        super(src);
    }
    
    /**
     * In the rule we defined a datatype HomeownerArray, than defined a datatype SasriaData, that contains as a 
     * field an array of HomeownerArray. Than using Data table mechanism we initialized a number of test data.
     * Decision table "Rules String testArrays(SasriaData sasriaData)" gets an SasriaData object as parameter, and works with 
     * HomeownerArray field. Also a test method was written for this decision table(it was done to prevent using generated
     * java classes beans of datatypes, as it is some sort of hardcode).
     * Using Openl API we invokes this test method, to test that Openl correctly works with arrays of user defined 
     * datatypes.
     * 
     */
    @Test
    public void testCase1() {
        IOpenClass __class = getJavaWrapper().getOpenClassWithErrors(); 
        
        IOpenMethod testMethod = __class.getMatchingMethod("testArraysTestTestAll", new IOpenClass[] {});
        
        Object[] __params = new Object[0];
        org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();
        Object __myInstance = __class.newInstance(environment);
        Object __res = testMethod.invoke(__myInstance, __params, environment);
        TestUnitsResults testUnitsResult = (TestUnitsResults) __res;        
        assertEquals(0, testUnitsResult.getNumberOfFailures());
    }
    
    /**
     * Test accessing datatype array via user defined string index (e.g. people["David"])
     */
    @Test
    public void testStringUserIndex() {
        IOpenClass __class = getJavaWrapper().getOpenClassWithErrors(); 
        
        IOpenMethod testMethod = __class.getMatchingMethod("foo", new IOpenClass[] {});
        
        Object[] __params = new Object[0];
        org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();
        Object __myInstance = __class.newInstance(environment);
        Object __res = testMethod.invoke(__myInstance, __params, environment);
                
        assertEquals("passed", __res.toString());
    }
    
    @Test
    public void testUnsupportedIndexCall() {
        IOpenClass __class = getJavaWrapper().getOpenClassWithErrors(); 
        
        IOpenMethod testMethod = __class.getMatchingMethod("foo2", new IOpenClass[] {});
        
        Object[] __params = new Object[0];
        org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();
        Object __myInstance = __class.newInstance(environment);
        
        try {
            testMethod.invoke(__myInstance, __params, environment);
            fail();
        } catch (NullPointerException e) {
            // fail during invoking, as we don`t support Object index, in datatype arrays.
            assertTrue(true);
        }
    }

}
