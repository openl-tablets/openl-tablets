package org.openl.rules.tbasic;

import java.io.File;

import org.junit.Ignore;
import org.openl.rules.TestHelper;

@Ignore("Manual test")
public class Test0 {
    public static void main(String[] args) {
        Test0 t = new Test0();
        t._test();
    }

    private void _test() {
        okRows(new File("test/rules/Algorithm.xls"), 26);
    }

    public Exception catchEx(File xlsFile) {
        Exception result = null;
        try {
            TestHelper<ITestAlgorithm1> testHelper;
            testHelper = new TestHelper<ITestAlgorithm1>(xlsFile, ITestAlgorithm1.class);

            testHelper.getTableSyntaxNode();
        } catch (Exception e) {
            result = e;
        }

        return result;
    }

    public void okRows(File xlsFile, int expectedNumberOfRows) {
        TestHelper<ITestAlgorithm1> testHelper;
        testHelper = new TestHelper<ITestAlgorithm1>(xlsFile, ITestAlgorithm1.class);
        ITestAlgorithm1 a = testHelper.getInstance();

        // assertEquals(expectedNumberOfRows, a.modification());
        a.modification();
    }
}
