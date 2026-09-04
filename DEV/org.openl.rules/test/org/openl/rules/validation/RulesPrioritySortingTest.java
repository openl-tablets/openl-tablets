package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;

class RulesPrioritySortingTest {
    public interface ITestI {
        Double driverRiskScoreOverloadTest(IRulesRuntimeContext context, String driverRisk);

        Double driverRiskScoreOverloadTest2(IRulesRuntimeContext context, String driverRisk);

        Double driverRiskScoreOverloadTest3(IRulesRuntimeContext context, String driverRisk);

        Double driverRiskScoreNoOverloadTest(IRulesRuntimeContext context, String driverRisk);
    }

    @Test
    void testStartRequestDate() throws Exception {
        ITestI instance = TestUtils.create("test/rules/overload/MaxMinOverload.xls", ITestI.class);
        var context = new DefaultRulesRuntimeContext();

        Object[][] testData = {{"2011-01-15", "2011-02-15", 120.0},
                {"2011-02-15", "2011-01-15", 120.0},
                {"2011-01-15", "2020-01-15", 120.0},
                {"2020-01-15", "2011-01-15", 120.0},
                {"2011-03-15", "2011-03-15", 120.0},
                {"2011-04-15", "2011-03-15", 100.0},
                {"2020-04-15", "2011-03-15", 100.0},
                {"2011-04-15", "2020-03-15", 100.0},
                {"2011-07-15", "2011-07-15", 100.0},
                {"2020-07-15", "2011-07-15", 100.0},
                {"2011-07-15", "2011-07-15", 100.0},
                {"2020-07-15", "2011-07-15", 100.0},
                {"2011-07-15", "2011-08-15", 150.0}};

        var df = new SimpleDateFormat("yyyy-MM-dd");

        for (var i = 0; i < testData.length; i++) {
            var data = testData[i];
            var currentDate = df.parse((String) data[0]);
            var requestDate = df.parse((String) data[1]);
            context.setCurrentDate(currentDate);
            context.setRequestDate(requestDate);
            var res = instance.driverRiskScoreOverloadTest(context, "High Risk Driver");
            assertEquals((Double) data[2], res.doubleValue(), 0, "testData index = " + i);
        }
    }

    @Test
    void testEndRequestDate() throws Exception {
        ITestI instance = TestUtils.create("test/rules/overload/MaxMinOverload.xls", ITestI.class);
        var context = new DefaultRulesRuntimeContext();

        Object[][] testData = {{"2011-08-15", "2012-01-01", 4.0}, {"2011-08-15", "2009-01-01", 2.0}};

        var df = new SimpleDateFormat("yyyy-MM-dd");

        for (var i = 0; i < testData.length; i++) {
            var data = testData[i];
            var currentDate = df.parse((String) data[0]);
            var requestDate = df.parse((String) data[1]);
            context.setCurrentDate(currentDate);
            context.setRequestDate(requestDate);
            var res = instance.driverRiskScoreOverloadTest2(context, "High Risk Driver");
            assertEquals((Double) data[2], res.doubleValue(), 0, "testData index = " + i);
        }
    }

    @Test
    void testFilledPropertiesSorting() throws Exception {
        ITestI instance = TestUtils.create("test/rules/overload/MaxMinOverload.xls", ITestI.class);
        var context = new DefaultRulesRuntimeContext();

        Object[][] testData = {{"2011-08-15", "lobb", 4.0}, {"2013-08-15", "lobb2", 6.0}};

        var df = new SimpleDateFormat("yyyy-MM-dd");

        for (var i = 0; i < testData.length; i++) {
            var data = testData[i];
            var currentDate = df.parse((String) data[0]);
            var lob = (String) data[1];
            context.setCurrentDate(currentDate);
            context.setLob(lob);
            var res = instance.driverRiskScoreOverloadTest3(context, "High Risk Driver");
            assertEquals((Double) data[2], res.doubleValue(), 0, "testData index = " + i);
        }
    }
}
