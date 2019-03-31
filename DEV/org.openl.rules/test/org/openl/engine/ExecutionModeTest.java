package org.openl.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.overload.OverloadTest.ITestI;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class ExecutionModeTest {
    @Test
    public void testDTExecution() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("./test/rules/Tutorial_4_Test.xls");
        engineFactory.setExecutionMode(true);
        Class<?> interfaceClass = engineFactory.getInterfaceClass();
        Method method = interfaceClass.getMethod("ageSurcharge", int.class);
        assertNotNull(method);
        Object instance = engineFactory.newInstance();
        Object result = method.invoke(instance, 2);
        assertEquals(new DoubleValue(300), result);
    }

    @Test
    public void testMethodExecution() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("./test/rules/Tutorial_4_Test.xls");
        engineFactory.setExecutionMode(true);

        Class<?> interfaceClass = engineFactory.getInterfaceClass();
        Method method = interfaceClass.getMethod("currentYear");
        assertNotNull(method);
        Object instance = engineFactory.newInstance();
        Object result = method.invoke(instance);
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), result);
    }

    @Test
    public void testTBasicExecution() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(
            "./test/rules/algorithm/Test_Factorial.xls");
        engineFactory.setExecutionMode(true);

        Class<?> interfaceClass = engineFactory.getInterfaceClass();
        Method method = interfaceClass.getMethod("modification", int.class);
        assertNotNull(method);
        Object instance = engineFactory.newInstance();
        Object result = method.invoke(instance, 5);
        assertEquals(120, result);

        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod openMethod = moduleOpenClass.getMethod("modification", new IOpenClass[] { JavaOpenClass.INT });
        if (openMethod instanceof Algorithm) {
            assertNull(((Algorithm) openMethod).getBoundNode());
        } else {
            if (openMethod instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) openMethod;
                for (IOpenMethod m : openMethodDispatcher.getCandidates()) {
                    if (m instanceof Algorithm) {
                        assertNull(((Algorithm) m).getBoundNode());
                    }
                }
            }
        }
    }

    @Test
    public void testSpreadsheetExecution() throws NoSuchMethodException,
                                           InvocationTargetException,
                                           IllegalAccessException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(
            "./test/rules/calc1/SpreadsheetResult_SimpleBean_Test.xls");
        engineFactory.setExecutionMode(true);

        Class<?> interfaceClass = engineFactory.getInterfaceClass();
        Method method = interfaceClass.getMethod("calc");
        assertNotNull(method);
        Object instance = engineFactory.newInstance();
        Object result = method.invoke(instance);
        assertEquals(new DoubleValue(375), result);

        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod openMethod = moduleOpenClass.getMethod("calc", new IOpenClass[] {});
        if (openMethod instanceof Spreadsheet) {
            assertNull(((Spreadsheet) openMethod).getBoundNode());
        } else {
            if (openMethod instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) openMethod;
                for (IOpenMethod m : openMethodDispatcher.getCandidates()) {
                    if (m instanceof Spreadsheet) {
                        assertNull(((Spreadsheet) m).getBoundNode());
                    }
                }
            }
        }
    }

    @Test
    public void testColumnMatchExecution() throws NoSuchMethodException,
                                           InvocationTargetException,
                                           IllegalAccessException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("./test/rules/cmatch1/match4-1.xls");
        engineFactory.setExecutionMode(true);

        Class<?> interfaceClass = engineFactory.getInterfaceClass();
        Method method = interfaceClass.getMethod("runColumnMatch", int.class, int.class, int.class, int.class);
        assertNotNull(method);
        Object instance = engineFactory.newInstance();
        Object result = method.invoke(instance, 4, 3, 3, 2);
        assertEquals(91, result);

        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod openMethod = moduleOpenClass.getMethod("runColumnMatch",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.INT, JavaOpenClass.INT, JavaOpenClass.INT });
        if (openMethod instanceof ColumnMatch) {
            assertNull(((ColumnMatch) openMethod).getBoundNode());
        } else {
            if (openMethod instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) openMethod;
                for (IOpenMethod m : openMethodDispatcher.getCandidates()) {
                    if (m instanceof ColumnMatch) {
                        assertNull(((ColumnMatch) m).getBoundNode());
                    }
                }
            }
        }
    }

    @Test
    public void testOverloaded() {
        RulesEngineFactory<ITestI> engineFactory = new RulesEngineFactory<ITestI>("test/rules/overload/Overload.xls",
            ITestI.class);
        engineFactory.setExecutionMode(true);

        ITestI instance = (ITestI) engineFactory.newInstance();

        IRulesRuntimeContext context = ((IRulesRuntimeContextProvider) instance).getRuntimeContext();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);

        context.setCurrentDate(calendar.getTime());

        DoubleValue res1 = instance.driverRiskScoreOverloadTest("High Risk Driver");
        assertEquals(120.0, res1.doubleValue(), 0);

        calendar.set(2008, 5, 15);
        context.setCurrentDate(calendar.getTime());

        DoubleValue res2 = instance.driverRiskScoreOverloadTest("High Risk Driver");
        assertEquals(100.0, res2.doubleValue(), 0);

        DoubleValue res3 = instance.driverRiskScoreNoOverloadTest("High Risk Driver");
        assertEquals(200.0, res3.doubleValue(), 0);
    }

    @Test
    public void testSkipedTables() {
        // in execution mode test tables and run tables have to be skipped
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(
            "./test/rules/testmethod/UserExceptionTest.xlsx");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenField testMethod = moduleOpenClass.getField("driverRiskTest1");
        assertNull(testMethod);
        RulesEngineFactory<?> factory2 = new RulesEngineFactory<Object>(
            "./test/rules/overload/RunMethodOverloadSupport.xls");
        factory2.setExecutionMode(true);
        IOpenClass moduleOpenClass2 = factory2.getCompiledOpenClass().getOpenClass();
        IOpenField runMethod = moduleOpenClass2.getField("driverRiskTest");
        assertNull(runMethod);
    }

    @Test(expected = InvocationTargetException.class)
    public void testRuntimeErrors() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("./test/rules/dt/RuntimeErrorTest.xls");
        engineFactory.setExecutionMode(true);

        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod methodWithError = moduleOpenClass.getMethod("npe",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        assertNotNull(methodWithError);

        Class<?> interfaceClass = engineFactory.getInterfaceClass();
        Method method = interfaceClass.getMethod("npe", Integer.class);
        assertNotNull(method);
        Object instance = engineFactory.newInstance();
        method.invoke(instance, new Object[] { null });
    }
}
