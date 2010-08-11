package org.openl.rules.overload;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.util.Calendar;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;

public class ActiveMethodFeatureTest {

    public interface ITestI {
        DoubleValue driverRiskScoreOverloadTest1(String driverRisk);
        DoubleValue driverRiskScoreOverloadTest2(String driverRisk);
    }

    @Test
    public void testMethodOverload1() {
        File xlsFile = new File("test/rules/overload/ActiveMethodFeature.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI instance = testHelper.getInstance();
        IRuntimeEnv env = ((IEngineWrapper) instance).getRuntimeEnv();

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        env.setContext(context);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        DoubleValue res1 = instance.driverRiskScoreOverloadTest1("High Risk Driver");
        assertEquals(120.0, res1.doubleValue());

        calendar.set(2008, 5, 15);
        context.setCurrentDate(calendar.getTime());

        DoubleValue res2 = instance.driverRiskScoreOverloadTest1("High Risk Driver");
        assertEquals(100.0, res2.doubleValue());
    }
    
    @Test
    public void testMethodOverload2() {
        File xlsFile = new File("test/rules/overload/ActiveMethodFeature.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI instance = testHelper.getInstance();
        IRuntimeEnv env = ((IEngineWrapper) instance).getRuntimeEnv();

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        env.setContext(context);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        try {
            DoubleValue res1 = instance.driverRiskScoreOverloadTest2("High Risk Driver");
        } catch (Exception e) {
            TestUtils.assertEx(e, "No matching methods for the context");
        }
        
    }
}
