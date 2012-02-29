package org.openl.rules.binding;

import static org.junit.Assert.*;

import java.io.File;
import org.junit.Before;
import org.junit.Test;

import org.openl.rules.TestHelper;


public class VarArgsMethodBindingTest {
    private static final String src = "test/rules/binding/VarArgsMethodBindingTest.xlsx";
    
    private static TestInterf instance;
    
    public interface TestInterf {
        boolean test1Argument();
        boolean test2Arguments();
        int test3Arguments();
        int testArrayOfArrays();
    }
    
    @Before
    public void init() {
        if (instance == null) {
            File xlsFile = new File(src);
            TestHelper<TestInterf> testHelper;
            testHelper = new TestHelper<TestInterf>(xlsFile, TestInterf.class);
            
            instance = testHelper.getInstance();    
        }  
    }
    
    @Test
    public void test1Argument1() {
        assertTrue(instance.test1Argument());
    }
    
    @Test
    public void test2Arguments() {
        assertTrue(instance.test2Arguments());
    }
    
    @Test
    public void test3Arguments() {
        assertEquals(11, instance.test3Arguments());
    }
    
    @Test
    public void testSummary() {
        assertEquals(10, instance.testArrayOfArrays());
    }
    
}
