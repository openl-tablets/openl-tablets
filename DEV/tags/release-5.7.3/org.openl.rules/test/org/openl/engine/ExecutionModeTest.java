package org.openl.engine;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Calendar;

import org.junit.Test;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.method.TableMethod;
import org.openl.rules.overload.OverloadTest.ITestI;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.rules.tbasic.Algorithm;
import org.openl.runtime.EngineFactory;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class ExecutionModeTest {
    @Test
    public void testDTExecution() {
        ApiBasedRulesEngineFactory engineFactory = new ApiBasedRulesEngineFactory("./test/rules/Tutorial_4_Test.xls");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod method = moduleOpenClass.getMatchingMethod("ageSurcharge", new IOpenClass[] { JavaOpenClass.INT });
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        assertEquals(new DoubleValue(300), method.invoke(moduleOpenClass.newInstance(env), new Object[] { 2 }, env));
        assertNull(method.getInfo().getSyntaxNode());
    }

    @Test
    public void testMethodExecution() {
        ApiBasedRulesEngineFactory engineFactory = new ApiBasedRulesEngineFactory("./test/rules/Tutorial_4_Test.xls");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod method = moduleOpenClass.getMatchingMethod("currentYear", new IOpenClass[] {});
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), method.invoke(moduleOpenClass.newInstance(env),
                new Object[] {}, env));
        assertNull(((TableMethod) method).getMethodTableBoundNode());
    }

    @Test
    public void testTBasicExecution() {
        ApiBasedRulesEngineFactory engineFactory = new ApiBasedRulesEngineFactory(
                "./test/rules/algorithm/Test_Factorial.xls");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod method = moduleOpenClass.getMatchingMethod("modification", new IOpenClass[] { JavaOpenClass.INT });
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        assertEquals(120, method.invoke(moduleOpenClass.newInstance(env), new Object[] { 5 }, env));
        assertNull(((Algorithm) method).getNode());
    }

    @Test
    public void testSpreadsheetExecution() {
        ApiBasedRulesEngineFactory engineFactory = new ApiBasedRulesEngineFactory(
                "./test/rules/calc1/SpreadsheetResult_SimpleBean_Test.xls");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod method = moduleOpenClass.getMatchingMethod("calc", new IOpenClass[] {});
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        assertEquals(new DoubleValue(375), method.invoke(moduleOpenClass.newInstance(env), new Object[] {}, env));
        assertNull(((Spreadsheet) method).getBoundNode());
    }

    @Test
    public void testColumnMatchExecution() {
        ApiBasedRulesEngineFactory engineFactory = new ApiBasedRulesEngineFactory("./test/rules/cmatch1/match4-1.xls");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod method = moduleOpenClass.getMatchingMethod("runColumnMatch", new IOpenClass[] { JavaOpenClass.INT,
                JavaOpenClass.INT, JavaOpenClass.INT, JavaOpenClass.INT });
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        assertEquals(91, method.invoke(moduleOpenClass.newInstance(env), new Object[] { 4, 3, 3, 2 }, env));
        assertNull(((ColumnMatch) method).getBoundNode());
    }

    @Test
    public void testOverloaded() {
        File xlsFile = new File("test/rules/overload/Overload.xls");
        EngineFactory<ITestI> engineFactory = new RuleEngineFactory<ITestI>(xlsFile, ITestI.class);
        engineFactory.setExecutionMode(true);

        ITestI instance = engineFactory.makeInstance();
        
        IRulesRuntimeContext context = ((IRulesRuntimeContextProvider) instance).getRuntimeContext();
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 15);
        
        context.setCurrentDate(calendar.getTime());
        
        DoubleValue res1 = instance.driverRiskScoreOverloadTest("High Risk Driver");
        assertEquals(120.0, res1.doubleValue());
        
        calendar.set(2008, 5, 15);
        context.setCurrentDate(calendar.getTime());
        
        DoubleValue res2 = instance.driverRiskScoreOverloadTest("High Risk Driver");
        assertEquals(100.0, res2.doubleValue());
        
        DoubleValue res3 = instance.driverRiskScoreNoOverloadTest("High Risk Driver");
        assertEquals(200.0, res3.doubleValue());
    }

    @Test
    public void testSkipedTables() {
        //in execution mode test tables and run tables have to be skipped
        ApiBasedRulesEngineFactory engineFactory = new ApiBasedRulesEngineFactory("./test/rules/testmethod/UserExceptionTest.xlsx");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenField testMethod = moduleOpenClass.getField("driverRiskTest1");
        assertNull(testMethod);
        ApiBasedRulesEngineFactory engineFactory2 = new ApiBasedRulesEngineFactory("./test/rules/overload/RunMethodOverloadSupport.xls");
        engineFactory2.setExecutionMode(true);
        IOpenClass moduleOpenClass2 = engineFactory2.getCompiledOpenClass().getOpenClass();
        IOpenField runMethod = moduleOpenClass2.getField("driverRiskTest");
        assertNull(runMethod);
    }

    @Test
    public void testRuntimeErrors() {
        ApiBasedRulesEngineFactory engineFactory = new ApiBasedRulesEngineFactory("./test/rules/dt/RuntimeErrorTest.xls");
        engineFactory.setExecutionMode(true);
        IOpenClass moduleOpenClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IOpenMethod methodWithError = moduleOpenClass.getMethod("getStrLength", new IOpenClass[] { JavaOpenClass.INT });
        assertNotNull(methodWithError);
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        try {
            methodWithError.invoke(moduleOpenClass.newInstance(env), new Object[] { 5 }, env);
        } catch (OpenLRuntimeException e) {
            assertNotNull(e.getSourceModule());
        } catch (Throwable t) {
            assertFalse(true);
        }
    }
}
