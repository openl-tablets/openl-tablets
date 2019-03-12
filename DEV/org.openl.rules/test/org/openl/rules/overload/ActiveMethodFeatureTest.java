package org.openl.rules.overload;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;

public class ActiveMethodFeatureTest {

    @Test
    public void testMethodOverload1() {

        ITestI instance = TestUtils.create("test/rules/overload/ActiveMethodFeature.xls", ITestI.class);
        IRuntimeEnv env = ((IEngineWrapper) instance).getRuntimeEnv();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        env.setContext(context);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        DoubleValue res1 = instance.driverRiskScoreOverloadTest1("High Risk Driver");
        assertEquals(120.0, res1.doubleValue(), 1e-8);

        calendar.set(2008, 5, 15);
        context.setCurrentDate(calendar.getTime());

        DoubleValue res2 = instance.driverRiskScoreOverloadTest1("High Risk Driver");
        assertEquals(100.0, res2.doubleValue(), 1e-8);
    }

    @Test
    public void testMethodOverload2() {

        ITestI instance = TestUtils.create("test/rules/overload/ActiveMethodFeature.xls", ITestI.class);
        IRuntimeEnv env = ((IEngineWrapper) instance).getRuntimeEnv();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        env.setContext(context);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        try {
            instance.driverRiskScoreOverloadTest2("High Risk Driver");
        } catch (Exception e) {
            TestUtils.assertEx(e, "No matching methods for the context");
        }

    }

    public interface ITestI {
        DoubleValue driverRiskScoreOverloadTest1(String driverRisk);

        DoubleValue driverRiskScoreOverloadTest2(String driverRisk);
    }
}
