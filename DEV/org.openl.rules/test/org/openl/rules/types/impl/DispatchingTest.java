package org.openl.rules.types.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.LanguagesEnum;
import org.openl.runtime.IEngineWrapper;
import org.openl.vm.IRuntimeEnv;

public class DispatchingTest {
    private static final String RULES_SOURCE_FILE = "test/rules/dispatching/Dispatching.xls";
    private static final String NO_EXCEPTION = "Exception should be thrown, but was: ";
    private static final String AMBIGUOUS_METHOD_MESSAGE = "Ambiguous dispatch for method";

    private Rules instance;

    @Before
    public void setUp() {
        instance = TestUtils.create(RULES_SOURCE_FILE, Rules.class);
    }

    @Test
    public void testSimpleDispatching() {
        IRulesRuntimeContext context = initContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.ENG);
        assertEquals(1, instance.getSimplePriority());

        context = initContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.RUS);
        assertEquals(2, instance.getSimplePriority());
    }

    @Test
    public void testDispatching() {
        IRulesRuntimeContext context = initContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.USD);
        assertEquals("US.ENG.USD", instance.getPriority());

        context = initContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.GER);
        context.setCurrency(CurrenciesEnum.USD);
        assertEquals("US.USD", instance.getPriority());

        context = initContext();
        context.setCountry(CountriesEnum.GB);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.EUR);
        assertEquals("GB.EUR", instance.getPriority());

        context = initContext();
        context.setLang(LanguagesEnum.ITA);
        context.setCurrency(CurrenciesEnum.EUR);
        assertEquals("ITA.EUR", instance.getPriority());

        context = initContext();
        context.setCountry(CountriesEnum.GB);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.GBP);
        assertEquals("GB.EUR,GBP", instance.getPriority());

        context = initContext();
        context.setCountry(CountriesEnum.GB);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.AED);
        assertEquals("none", instance.getPriority());
    }

    @Test
    public void testRequestDate() throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        Object[][] testData = { { "2011-08-15", "2012-01-01", 4.0 }, { "2011-08-15", "2009-01-01", 2.0 } };

        for (int i = 0; i < testData.length; i++) {
            IRulesRuntimeContext context = initContext();
            Object[] data = testData[i];
            Date currentDate = df.parse((String) data[0]);
            Date requestDate = df.parse((String) data[1]);
            context.setCurrentDate(currentDate);
            context.setRequestDate(requestDate);
            DoubleValue res = instance.driverRiskScoreOverloadTest2("High Risk Driver");
            assertEquals("testData index = " + i, (Double) data[2], res.doubleValue(), 0);
        }
    }

    @Test
    public void testAmbiguousDispatching1() throws Exception {
        Method method = Rules.class.getMethod("getAmbiguousPriority");

        IRulesRuntimeContext context = initContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.ENG);
        invokeAndCheckForAmbiguous(method);
    }

    @Test
    public void testAmbiguousDispatching2() throws Exception {
        Method method = Rules.class.getMethod("getAmbiguousPriority1");

        IRulesRuntimeContext context = initContext();
        context.setCountry(CountriesEnum.RU);
        context.setLang(LanguagesEnum.ENG);
        invokeAndCheckForAmbiguous(method);
    }

    @Test
    public void testAmbiguousDispatching3() throws Exception {
        Method method = Rules.class.getMethod("getPriority");

        IRulesRuntimeContext context = initContext();
        invokeAndCheckForAmbiguous(method);

        context = initContext();
        context.setCountry(CountriesEnum.US);
        context.setCurrency(CurrenciesEnum.USD);
        invokeAndCheckForAmbiguous(method);
    }

    public void calcBenchmark() {
        IRuntimeEnv runtimeEnv = ((IEngineWrapper)instance).getRuntimeEnv();

        IRulesRuntimeContext context1 = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context1.setCountry(CountriesEnum.US);
        context1.setLang(LanguagesEnum.ENG);
        context1.setCurrency(CurrenciesEnum.USD);

        IRulesRuntimeContext context2 = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context2.setLang(LanguagesEnum.ITA);
        context2.setCurrency(CurrenciesEnum.EUR);

        IRulesRuntimeContext context3 = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context3.setCountry(CountriesEnum.US);
        context3.setLang(LanguagesEnum.GER);
        context3.setCurrency(CurrenciesEnum.USD);

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            runtimeEnv.setContext(context1);
            instance.getPriority();

            runtimeEnv.setContext(context2);
            instance.getPriority();

            runtimeEnv.setContext(context3);
            instance.getPriority();
        }
        long t2 = System.currentTimeMillis();

        System.out.println(t2 - t1);
    }

    private void invokeAndCheckForAmbiguous(Method method) throws IllegalAccessException {
        try {
            Object o = method.invoke(instance);
            fail(NO_EXCEPTION + o);
        } catch (InvocationTargetException e) {
            assertNotNull(e);

            StringWriter sw = new StringWriter(1024);
            PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            pw.close();

            assertTrue(sw.toString().contains(AMBIGUOUS_METHOD_MESSAGE));
        }
    }

    private IRulesRuntimeContext initContext() {
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        ((IEngineWrapper) instance).getRuntimeEnv().setContext(context);
        return context;
    }

    public interface Rules {
        int getSimplePriority();

        String getPriority();

        int getAmbiguousPriority();

        int getAmbiguousPriority1();

        DoubleValue driverRiskScoreOverloadTest2(String driverRisk);
    }
}
