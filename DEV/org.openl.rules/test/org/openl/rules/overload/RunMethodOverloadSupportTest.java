package org.openl.rules.overload;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class RunMethodOverloadSupportTest {

    public interface ITestI {
        void driverRiskTest();
    }

    @Test
    public void testMethodOverloadSupport() {
        File xlsFile = new File("test/rules/overload/RunMethodOverloadSupport.xls");
        TestHelper<ITestI> testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI instance = testHelper.getInstance();
        instance.driverRiskTest();
    }
}
