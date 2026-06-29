package org.openl.rules.overload;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.TestUnitsResults;

class RunMethodOverloadSupportTest {

    @Test
    void testMethodOverloadSupport() {
        ITestI instance = TestUtils.create("test/rules/overload/RunMethodOverloadSupport.xls", ITestI.class);
        instance.driverRiskTest();
    }

    public interface ITestI {
        TestUnitsResults driverRiskTest();
    }
}
