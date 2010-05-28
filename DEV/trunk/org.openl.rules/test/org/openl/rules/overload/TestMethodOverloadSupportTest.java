package org.openl.rules.overload;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;

public class TestMethodOverloadSupportTest {
    
    public interface ITestI {
        
        void driverRiskTestTestAll();
    }
    
    @Test
    public void testMethodOverloadSupport() {
        File xlsFile = new File("test/rules/overload/TestMethodOverloadSupport.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);
        
        ITestI instance = testHelper.getInstance();
        IRuntimeEnv env = ((IEngineWrapper) instance).getRuntimeEnv();

        instance.driverRiskTestTestAll();
    }
}
