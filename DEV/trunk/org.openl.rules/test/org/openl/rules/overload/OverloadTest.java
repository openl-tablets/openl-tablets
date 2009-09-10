package org.openl.rules.overload;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;
import org.openl.runtime.IContext;
import org.openl.runtime.IEngineWrapper;

public class OverloadTest {
	
	public interface ITestI {
		DoubleValue driverRiskScoreOverloadTest(String driverRisk);
	}
	
	@Test
	public void testMethodOverload() {
		File xlsFile = new File("test/rules/overload/Overload.xls");
		TestHelper<ITestI> testHelper;
		testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);
		
		ITestI instance = testHelper.getInstance();
		IContext context = ((IEngineWrapper<ITestI>) instance).getRuntimeEnv().getContext(); 

		Calendar calendar = Calendar.getInstance();
		calendar.set(2003, 5, 15);
		context.addValue("date", Date.class, calendar.getTime());
		
		DoubleValue res1 = instance.driverRiskScoreOverloadTest("High Risk Driver");
		assertEquals(120.0, res1.doubleValue());
		
		calendar.set(2008, 5, 15);
		context.setValue("date", Date.class, calendar.getTime());
		
		DoubleValue res2 = instance.driverRiskScoreOverloadTest("High Risk Driver");
		assertEquals(100.0, res2.doubleValue());
	}
}
