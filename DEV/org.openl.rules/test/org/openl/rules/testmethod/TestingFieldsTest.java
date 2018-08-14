package org.openl.rules.testmethod;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class TestingFieldsTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/testmethod/TestingFieldsTest.xls";

    public TestingFieldsTest() {
        super(SRC);
    }

    @Test
    public void checkTestTableResults() {
        TestUnitsResults testResults = (TestUnitsResults) invokeMethod("returnObjectTest");
        for (int i = 0; i < testResults.getTestUnits().size() - 2; i++) {
            assertEquals(testResults.getTestUnits().get(i).getResultStatus(), TestStatus.TR_NEQ);
        }
        for (int i = testResults.getTestUnits().size() - 2; i < testResults.getTestUnits().size(); i++) {
            assertEquals(testResults.getTestUnits().get(i).getResultStatus(), TestStatus.TR_OK);
        }
    }
}
