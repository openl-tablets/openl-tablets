package org.openl.rules.testmethod;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;

public class TestingFieldsTest extends BaseOpenlBuilderHelper {
    private static String __src = "test/rules/testmethod/TestingFieldsTest.xls";

    public TestingFieldsTest() {
        super(__src);
    }

    @Test
    public void checkTestTableResults() {
        TestUnitsResults testResults = (TestUnitsResults) invokeMethod("returnObjectTestTestAll");
        for (int i = 0; i < testResults.getTestUnits().size() - 2; i++) {
            assertEquals(testResults.getTestUnits().get(i).compareResult(), TestStatus.TR_NEQ.getStatus());
        }
        for (int i = testResults.getTestUnits().size() - 2; i < testResults.getTestUnits().size(); i++) {
            assertEquals(testResults.getTestUnits().get(i).compareResult(), TestStatus.TR_OK.getStatus());
        }
    }
}
