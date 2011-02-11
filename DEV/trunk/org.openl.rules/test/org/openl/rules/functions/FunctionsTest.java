package org.openl.rules.functions;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.helpers.RulesUtils;

public class FunctionsTest {
    
    private static final String src = "test/rules/FunctionsTest.xlsx";
    
    private static TestInterf instance;
    
    public interface TestInterf {
        String testMaxByte(byte[] obj);        
        String testMaxInteger(Integer[] obj);
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
    public void testByteMax() {
        assertEquals("res2", instance.testMaxByte(new byte[]{(byte)3, (byte)5}));
    }
    
    @Test
    public void testIntegerMax() {
        assertEquals("res2", instance.testMaxInteger(new Integer[]{Integer.valueOf("120"), Integer.valueOf("200")}));
    }
    
    @Test
    public void test() {
        assertEquals(Integer.valueOf("120"), RulesUtils.min(new Integer[]{Integer.valueOf("120"), Integer.valueOf("200")}));
    }
    
}
    