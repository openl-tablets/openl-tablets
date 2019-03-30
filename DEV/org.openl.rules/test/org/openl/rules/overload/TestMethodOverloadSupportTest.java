package org.openl.rules.overload;

import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.TestUnitsResults;

public class TestMethodOverloadSupportTest {

    @Test
    public void testMethodOverloadSupport() {
        ITestI instance = TestUtils.create("test/rules/overload/TestMethodOverloadSupport.xls", ITestI.class);
        instance.driverRiskTest();
    }

    public interface ITestI {
        TestUnitsResults driverRiskTest();
    }
}
