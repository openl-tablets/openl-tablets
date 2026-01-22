package org.openl.rules.overload;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;

public class ActiveMethodFeatureTest {

    @Test
    public void testMethodOverload1() {

        ITestI instance = TestUtils.create("test/rules/overload/ActiveMethodFeature.xls", ITestI.class);

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        Double res1 = instance.driverRiskScoreOverloadTest1(context, "High Risk Driver");
        assertEquals(120.0, res1, 1e-8);

        calendar.set(2008, 5, 15);
        context.setCurrentDate(calendar.getTime());

        Double res2 = instance.driverRiskScoreOverloadTest1(context, "High Risk Driver");
        assertEquals(100.0, res2, 1e-8);
    }

    @Test
    public void testMethodOverload2() {

        ITestI instance = TestUtils.create("test/rules/overload/ActiveMethodFeature.xls", ITestI.class);

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        try {
            instance.driverRiskScoreOverloadTest2(context, "High Risk Driver");
        } catch (Exception e) {
            TestUtils.assertEx(e, "No matching methods for the context");
        }

    }

    public interface ITestI {
        Double driverRiskScoreOverloadTest1(IRulesRuntimeContext context, String driverRisk);

        Double driverRiskScoreOverloadTest2(IRulesRuntimeContext context, String driverRisk);
    }
}
