package org.openl.rules.calc;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestHelper;

public class TestAutoType3 {
    public interface ITestCalc {
        SpreadsheetResult calc3();
    }
    
    
    @Before
    public void before() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "false");
    }


    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc/autotype/autotype-3.xls");
        TestHelper<ITestCalc> testHelper = null;
        Exception ex = null;
        
        try {
            testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);
            
		} catch (Exception e) {
			ex = e;
		}
        
        
        Assert.assertNotNull(ex);
        Assert.assertNull(testHelper);


    }
}
