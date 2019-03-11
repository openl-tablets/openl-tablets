package org.openl.rules.engine;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;

public class RuntimeContextTest {

    @Test
    public void testEngineRulesContext() {
        ITestI instance = TestUtils.create("test/rules/engine/RulesContextTest.xls", ITestI.class);
        IRulesRuntimeContext context = instance.getRuntimeContext();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        DoubleValue res1 = instance.driverRiskScoreOverloadTest("High Risk Driver");
        assertEquals(120.0, res1.doubleValue(), 1e-8);

        calendar.set(2008, 5, 15);
        context.setCurrentDate(calendar.getTime());

        DoubleValue res2 = instance.driverRiskScoreOverloadTest("High Risk Driver");
        assertEquals(100.0, res2.doubleValue(), 1e-8);

        DoubleValue res3 = instance.driverRiskScoreNoOverloadTest("High Risk Driver");
        assertEquals(200.0, res3.doubleValue(), 1e-8);
    }

    public interface ITestI extends IRulesRuntimeContextProvider {
        DoubleValue driverRiskScoreOverloadTest(String driverRisk);

        DoubleValue driverRiskScoreNoOverloadTest(String driverRisk);
    }
}
