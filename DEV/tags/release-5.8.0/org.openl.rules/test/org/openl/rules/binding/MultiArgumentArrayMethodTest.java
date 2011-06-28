package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.TestHelper;

public class MultiArgumentArrayMethodTest {
    private static final String src = "test/rules/binding/MultiArgumentArrayMethodTest.xls";
    
    private static MultiArgumentArrayMethodInterf instance;
    
    public interface MultiArgumentArrayMethodInterf {               
        int callMultiArguments();
        int[] callMultiArgumentsArray();
        int[] callMultiArgumentsArray1();        
        int[] callMultiArgumentsArray2();
        int[] testArrayCall1();
    }
    
    @Before
    public void init() {
        if (instance == null) {
            File xlsFile = new File(src);
            TestHelper<MultiArgumentArrayMethodInterf> testHelper;
            testHelper = new TestHelper<MultiArgumentArrayMethodInterf>(xlsFile, MultiArgumentArrayMethodInterf.class);
            
            instance = testHelper.getInstance();    
        }  
    }
    
    @Test
    public void testMultiArgumentsCall() {
        assertEquals(16, instance.callMultiArguments());
    }
    
    @Test
    public void testMultiArgumentsArrayCall() {
        // test calling multi arguments method with 1 array argument
        assertEquals(2, instance.callMultiArgumentsArray().length);
        assertEquals(16, instance.callMultiArgumentsArray()[0]);
        assertEquals(17, instance.callMultiArgumentsArray()[1]);
    }
    
    @Test
    public void testMultiArgumentsArrayCall1() {
        // test calling multi arguments method with 2 array argument
        assertEquals(2, instance.callMultiArgumentsArray1().length);
        assertEquals(26, instance.callMultiArgumentsArray1()[0]);
        assertEquals(27, instance.callMultiArgumentsArray1()[1]);
    }
    
    @Test
    public void testMultiArgumentsArrayCall2() {
        // test calling multi arguments method with 3 array argument
        assertEquals(3, instance.callMultiArgumentsArray2().length);
        assertEquals(25, instance.callMultiArgumentsArray2()[0]);
        assertEquals(26, instance.callMultiArgumentsArray2()[1]);
        assertEquals(27, instance.callMultiArgumentsArray2()[2]);
    }
    
    @Test
    public void testArrayCall1() {
        // check calling array of arrays
        assertEquals(2, instance.testArrayCall1().length);
        assertEquals(30, instance.testArrayCall1()[0]);
        assertEquals(31, instance.testArrayCall1()[1]);
    }
}
