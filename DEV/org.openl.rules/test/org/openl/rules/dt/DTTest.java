package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class DTTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "./test/rules/dt/DTTest.xlsx";

    public DTTest() {
        super(SRC);
    }

    @Test
    public void greeting() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("Greeting",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(0) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(11) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(12) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(15) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(17) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(18) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(20) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(21) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(22) }, env);
        assertEquals("Good Night, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(23) }, env);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting1() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("Greeting1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(0) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(11) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(12) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(15) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(17) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(18) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(20) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(21) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(22) }, env);
        assertEquals("Good Night, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(23) }, env);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting2() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("Greeting2",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(0) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(11) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(12) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(15) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(17) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(18) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(20) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(21) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(22) }, env);
        assertEquals("Good Night, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(23) }, env);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting3() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("Greeting3",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(0) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(11) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(12) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(15) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(17) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(18) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(20) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(21) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(22) }, env);
        assertEquals("Good Night, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(23) }, env);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greeting4() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("Greeting4",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(0) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(11) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(12) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(15) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(17) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(18) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(20) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(21) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(22) }, env);
        assertEquals("Good Night, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(23) }, env);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void Greeting6() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("Greeting6",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(0) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(11) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(12) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(15) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(17) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(18) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(20) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(21) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(22) }, env);
        assertEquals("Good Night, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(23) }, env);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void greetingTwoRet2() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("GreetingTwoRet2",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(0) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(11) }, env);
        assertEquals("Good Morning, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(12) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(15) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(17) }, env);
        assertEquals("Good Afternoon, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(18) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(20) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(21) }, env);
        assertEquals("Good Evening, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(22) }, env);
        assertEquals("Good Night, World!", result);
        result = method.invoke(instance, new Object[] { new Integer(23) }, env);
        assertEquals("Good Night, World!", result);
    }

    @Test
    public void driverPremium1() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremium1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Young Driver", "Married" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Young Driver", "Single" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Married" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Single" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "" }, env);
        assertNull(result);

    }

    @Test
    public void driverPremiumTwoRet1() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremiumTwoRet1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Young Driver", "Married" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Young Driver", "Single" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Married" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Single" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "" }, env);
        assertNull(result);
    }

    @Test
    public void driverPremium2() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremium2",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Young Driver", "Married" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Young Driver", "Single" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Married" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Single" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "" }, env);
        assertNull(result);
    }

    @Test
    public void driverPremium3() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremium3",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Young Driver", "Married" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Young Driver", "Single" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Married" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Single" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "" }, env);
        assertNull(result);
    }

    @Test
    public void driverPremium7() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremium7",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Married", "Young Driver" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Single", "Young Driver" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Married", "Senior Driver" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Single", "Senior Driver" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "", "Standard Driver" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "" }, env);
        assertNull(result);
    }

    @Test
    public void driverPremium4() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremium4",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Young Driver", "Married" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Young Driver", "Single" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Married" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Single" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "Single" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "Married" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "" }, env);
        assertNull(result);
    }

    @Test
    public void driverPremium5() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremium5",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Young Driver", "Married" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Young Driver", "Single" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Married" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "Single" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "Single" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "Married" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "" }, env);
        assertNull(result);
    }

    @Test
    public void driverPremium6() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("DriverPremium6",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class),
                    JavaOpenClass.getOpenClass(String.class),
                    JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "Young Driver", "", "Married" }, env);
        assertEquals(new DoubleValue(700), result);
        result = method.invoke(instance, new Object[] { "Young Driver", "", "Single" }, env);
        assertEquals(new DoubleValue(720), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "", "Married" }, env);
        assertEquals(new DoubleValue(300), result);
        result = method.invoke(instance, new Object[] { "Senior Driver", "", "Single" }, env);
        assertEquals(new DoubleValue(350), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "", "Single" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "Standard Driver", "", "Married" }, env);
        assertEquals(new DoubleValue(500), result);
        result = method.invoke(instance, new Object[] { "", "", "" }, env);
        assertNull(result);
    }

    @Test
    public void carPrice() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("CarPrice",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class),
                    JavaOpenClass.getOpenClass(String.class),
                    JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "USA", "BMW", "Z4 sDRIVE35i" }, env);
        assertEquals(new DoubleValue(55150), result);
        result = method.invoke(instance, new Object[] { "Belarus", "Porche", "911 Carrera 4" }, env);
        assertEquals(new DoubleValue(130030), result);
        result = method.invoke(instance, new Object[] { "", "", "" }, env);
        assertNull(result);
    }

    @Test
    public void carPrice2() throws Exception {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("CarPrice2",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class),
                    JavaOpenClass.getOpenClass(String.class),
                    JavaOpenClass.getOpenClass(String.class),
                    JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "", "USA", "BMW", "Z4 sDRIVE35i" }, env);
        assertEquals(new DoubleValue(55150), result);
        result = method.invoke(instance, new Object[] { "", "Belarus", "Porche", "911 Carrera 4" }, env);
        assertEquals(new DoubleValue(130030), result);
        result = method.invoke(instance, new Object[] { "", "", "", "" }, env);
        assertNull(result);
    }
}
