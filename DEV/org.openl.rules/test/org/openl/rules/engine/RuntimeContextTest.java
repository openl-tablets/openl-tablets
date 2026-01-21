package org.openl.rules.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;

public class RuntimeContextTest {

    @Test
    public void testEngineRulesContext() {
        ITestI instance = TestUtils.create("test/rules/engine/RulesContextTest.xls", ITestI.class);
        var context = new DefaultRulesRuntimeContext();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        Double res1 = instance.driverRiskScoreOverloadTest(context, "High Risk Driver");
        assertEquals(120.0, res1.doubleValue(), 1e-8);

        calendar.set(2008, 5, 15);
        context.setCurrentDate(calendar.getTime());

        Double res2 = instance.driverRiskScoreOverloadTest(context, "High Risk Driver");
        assertEquals(100.0, res2.doubleValue(), 1e-8);

        Double res3 = instance.driverRiskScoreNoOverloadTest(context, "High Risk Driver");
        assertEquals(200.0, res3.doubleValue(), 1e-8);
    }

    public interface ITestI {
        Double driverRiskScoreOverloadTest(IRulesRuntimeContext context, String driverRisk);

        Double driverRiskScoreNoOverloadTest(IRulesRuntimeContext context, String driverRisk);
    }
}
