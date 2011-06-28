package org.openl.rules.overload;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.util.Calendar;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;

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
}
