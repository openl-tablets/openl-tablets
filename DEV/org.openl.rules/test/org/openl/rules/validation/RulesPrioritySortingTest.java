package org.openl.rules.validation;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.rules.runtime.RulesEngineFactory;

public class RulesPrioritySortingTest {
    public interface ITestI extends IRulesRuntimeContextProvider {
        DoubleValue driverRiskScoreOverloadTest(String driverRisk);
        DoubleValue driverRiskScoreOverloadTest2(String driverRisk);
        DoubleValue driverRiskScoreOverloadTest3(String driverRisk);

        DoubleValue driverRiskScoreNoOverloadTest(String driverRisk);
    }

    @Test
    public void testStartRequestDate() throws Exception {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_DT);
        ITestI instance = TestUtils.create("test/rules/overload/MaxMinOverload.xls", ITestI.class);
        IRulesRuntimeContext context = instance.getRuntimeContext();

        Object[][] testData = { { "2011-01-15", "2011-02-15", 120.0 }, { "2011-02-15", "2011-01-15", 120.0 },
                { "2011-01-15", "2020-01-15", 120.0 }, { "2020-01-15", "2011-01-15", 120.0 },
                { "2011-03-15", "2011-03-15", 120.0 }, { "2011-04-15", "2011-03-15", 100.0 },
                { "2020-04-15", "2011-03-15", 100.0 }, { "2011-04-15", "2020-03-15", 100.0 },
                { "2011-07-15", "2011-07-15", 100.0 }, { "2020-07-15", "2011-07-15", 100.0 },
                { "2011-07-15", "2011-07-15", 100.0 }, { "2020-07-15", "2011-07-15", 100.0 },
                { "2011-07-15", "2011-08-15", 150.0 } };

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < testData.length; i++) {
            Object[] data = testData[i];
            Date currentDate = df.parse((String) data[0]);
            Date requestDate = df.parse((String) data[1]);
            context.setCurrentDate(currentDate);
            context.setRequestDate(requestDate);
            DoubleValue res = instance.driverRiskScoreOverloadTest("High Risk Driver");
            assertEquals("testData index = " + i, (Double) data[2], res.doubleValue(), 0);
        }
    }
    @Test
    public void testEndRequestDate() throws Exception {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_DT);
        ITestI instance = TestUtils.create("test/rules/overload/MaxMinOverload.xls", ITestI.class);
        IRulesRuntimeContext context = instance.getRuntimeContext();

        Object[][] testData = { { "2011-08-15", "2012-01-01",4.0 },{ "2011-08-15", "2009-01-01",2.0 }};

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < testData.length; i++) {
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
    public void testFilledPropertiesSorting() throws Exception {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_DT);
        ITestI instance = TestUtils.create("test/rules/overload/MaxMinOverload.xls", ITestI.class);
        IRulesRuntimeContext context = instance.getRuntimeContext();

        Object[][] testData = { { "2011-08-15", "lobb",4.0 },{ "2011-08-15", "lobb2",7.0 }};

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < testData.length; i++) {
            Object[] data = testData[i];
            Date currentDate = df.parse((String) data[0]);
            String lob = (String) data[1];
            context.setCurrentDate(currentDate);
            context.setLob(lob);
            DoubleValue res = instance.driverRiskScoreOverloadTest3("High Risk Driver");
            assertEquals("testData index = " + i, (Double) data[2], res.doubleValue(), 0);
        }
    }
}
