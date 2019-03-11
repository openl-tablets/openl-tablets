package org.openl.rules;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;

public class DataLoadingTest {

    private static final String SRC = "test/rules/DataLoading.xls";

    @Test
    public void testMethodOverload1() {
        ITestI instance = TestUtils.create(SRC, ITestI.class);
        IRuntimeEnv env = ((IEngineWrapper) instance).getRuntimeEnv();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        env.setContext(context);

        Date[] res = instance.getDateSet();
        Date[] expected = { new Date("12/31/2010"), new Date("1/1/2012") };

        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], res[i]);
        }

        Calendar[] resCal = instance.getCalendarSet();
        Calendar[] expectedCal = { cal(expected[0]), cal(expected[1]) };

        for (int i = 0; i < expectedCal.length; i++) {
            Assert.assertEquals(expectedCal[i], resCal[i]);
        }

    }

    private Calendar cal(Date date) {
        Calendar c = Calendar.getInstance(Locale.US);
        c.setTime(date);
        return c;
    }

    public interface ITestI {
        Date[] getDateSet();

        Calendar[] getCalendarSet();
    }

}
