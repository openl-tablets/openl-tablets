package org.openl.rules.dt;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class ArrayLoadInSingleCellTest {

    public interface ITestI {
        String test1(String code, int idx);
    }
    
    @Test
    public void testMultiRowArrayLoad() {
        File xlsFile = new File("test/rules/dt/SingleCellArrayLoadTest.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);
        
        ITestI instance = testHelper.getInstance();
        
        String s = instance.test1("d1", 0);
        assertEquals("d1-1", s);
        
        s = instance.test1("d2", 2);
        assertEquals("d2-3", s);
        s = instance.test1("d3", 1);
        assertEquals("d3-2", s);
        s = instance.test1("d4", 2);
        assertEquals("d4-3", s);
    }
    
    
    
    
}
