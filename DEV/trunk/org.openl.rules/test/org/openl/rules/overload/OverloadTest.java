package org.openl.rules.overload;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.validation.AuxiliaryMethodsValidator;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.types.IOpenMethod;

public class OverloadTest {

    public interface ITestI extends IRulesRuntimeContextProvider {
        DoubleValue driverRiskScoreOverloadTest(String driverRisk);

        DoubleValue driverRiskScoreNoOverloadTest(String driverRisk);
    }

    @Test
    public void testMethodOverload() {
        File xlsFile = new File("test/rules/overload/Overload.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI instance = testHelper.getInstance();
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
    public void testAuxiliaryMethods() {
        File xlsFile = new File("test/rules/overload/Overload.xls");
        TestHelper<ITestI> testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);
        CompiledOpenClass compiledOpenClass = testHelper.getEngineFactory().getCompiledOpenClass();
        List<IOpenMethod> methods = compiledOpenClass.getOpenClass().getMethods();
        List<OpenMethodDispatcher> dispatchers = new ArrayList<OpenMethodDispatcher>();
        for (IOpenMethod method : methods) {
            if (method instanceof OpenMethodDispatcher) {
                dispatchers.add((OpenMethodDispatcher) method);
            }
        }
        for (OpenMethodDispatcher dispatcher : dispatchers) {
            for (int i = 0; i < dispatcher.getCandidates().size(); i++) {
                assertTrue(compiledOpenClass.getOpenClass()
                    .getMethod(dispatcher.getName() + XlsModuleOpenClass.AUXILIARY_METHOD_DELIMETER + i,
                        dispatcher.getSignature().getParameterTypes()) != null);
            }
        }
    }

//    @Test  see comment at AuxiliaryMethodsValidator
    public void testAuxiliaryMethodsValidator() {
        File xlsFile = new File("test/rules/overload/AuxiliaryMethodsTest.xls");
        try{
            TestHelper<ITestI> testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);
            testHelper.getEngineFactory().getCompiledOpenClass();
            assertFalse(true);
        }catch (CompositeOpenlException e) {
            assertEquals(e.getErrorMessages()[0].getSummary(),AuxiliaryMethodsValidator.ERROR_MESSAGE);
        }
    }
}
