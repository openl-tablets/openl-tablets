package org.openl.rules.types.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.LanguagesEnum;

public class DispatchingTest {
    private static final String RULES_SOURCE_FILE = "test/rules/dispatching/Dispatching.xls";
    private static final String NO_EXCEPTION = "Exception should be thrown, but was: ";
    private static final String AMBIGUOUS_METHOD_MESSAGE = "Ambiguous dispatch for method";

    private Rules instance;

    @BeforeEach
    public void setUp() {
        instance = TestUtils.create(RULES_SOURCE_FILE, Rules.class);
    }

    @Test
    public void testSimpleDispatching() {
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.ENG);
        assertEquals(1, instance.getSimplePriority(context));

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.RUS);
        assertEquals(2, instance.getSimplePriority(context));
    }

    @Test
    public void testDispatching() {
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.USD);
        assertEquals("US.ENG.USD", instance.getPriority(context));

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.GER);
        context.setCurrency(CurrenciesEnum.USD);
        assertEquals("US.USD", instance.getPriority(context));

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.GB);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.EUR);
        assertEquals("GB.EUR", instance.getPriority(context));

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLang(LanguagesEnum.ITA);
        context.setCurrency(CurrenciesEnum.EUR);
        assertEquals("ITA.EUR", instance.getPriority(context));

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.GB);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.GBP);
        assertEquals("GB.EUR,GBP", instance.getPriority(context));

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.GB);
        context.setLang(LanguagesEnum.ENG);
        context.setCurrency(CurrenciesEnum.AED);
        assertEquals("none", instance.getPriority(context));
    }

    @Test
    public void testRequestDate() throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        Object[][] testData = {{"2011-08-15", "2012-01-01", 4.0}, {"2011-08-15", "2009-01-01", 2.0}};

        for (int i = 0; i < testData.length; i++) {
            IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
            Object[] data = testData[i];
            Date currentDate = df.parse((String) data[0]);
            Date requestDate = df.parse((String) data[1]);
            context.setCurrentDate(currentDate);
            context.setRequestDate(requestDate);
            Double res = instance.driverRiskScoreOverloadTest2(context, "High Risk Driver");
            assertEquals((Double) data[2], res.doubleValue(), 0, "testData index = " + i);
        }
    }

    @Test
    public void testAmbiguousDispatching1() throws Exception {
        Method method = Rules.class.getMethod("getAmbiguousPriority", IRulesRuntimeContext.class);

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        context.setLang(LanguagesEnum.ENG);
        invokeAndCheckForAmbiguous(method, instance, new Object[1]);
    }

    @Test
    public void testAmbiguousDispatching2() throws Exception {
        Method method = Rules.class.getMethod("getAmbiguousPriority1", IRulesRuntimeContext.class);

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.RU);
        context.setLang(LanguagesEnum.ENG);
        invokeAndCheckForAmbiguous(method, instance, new Object[1]);
    }

    @Test
    public void testAmbiguousDispatching3() throws Exception {
        Method method = Rules.class.getMethod("getPriority", IRulesRuntimeContext.class);

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        invokeAndCheckForAmbiguous(method, instance, context);

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        context.setCurrency(CurrenciesEnum.USD);
        invokeAndCheckForAmbiguous(method, instance, context);
    }

    @Test
    public void testDatesDispatching() {
        MyRule myRule = TestUtils.create("test/rules/dispatching/EPBDS-10367_dates_Dispatching.xlsx", MyRule.class);
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        assertEquals(myRule.myRule(13), (Double) 7.0);

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar cal = new GregorianCalendar();
        cal.set(2021, Calendar.OCTOBER, 4, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        context.setCurrentDate(cal.getTime());
        assertEquals(myRule.myRule(13), (Double) 7.0);

        context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        Calendar cal2 = new GregorianCalendar();
        cal2.set(2019, Calendar.OCTOBER, 4, 0, 0, 0);
        cal2.set(Calendar.MILLISECOND, 0);
        context.setCurrentDate(cal2.getTime());
        assertEquals(myRule.myRule(13), (Double) 7.0);
    }

    private void invokeAndCheckForAmbiguous(Method method, Object target, Object... args) throws IllegalAccessException {
        try {
            Object o = method.invoke(target, args);
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

    public interface Rules {
        int getSimplePriority(IRulesRuntimeContext context);

        String getPriority(IRulesRuntimeContext context);

        int getAmbiguousPriority(IRulesRuntimeContext context);

        int getAmbiguousPriority1(IRulesRuntimeContext context);

        Double driverRiskScoreOverloadTest2(IRulesRuntimeContext context, String driverRisk);
    }

    public interface MyRule {
        Double myRule(Integer a);
    }
}
