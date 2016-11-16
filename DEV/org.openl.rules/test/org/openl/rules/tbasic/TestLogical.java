package org.openl.rules.tbasic;

import java.io.File;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class TestLogical {
    public void okRows(File xlsFile, int param, int expectedResult) {
        TestHelper<ITestAlgorithm2> testHelper;
        testHelper = new TestHelper<ITestAlgorithm2>(xlsFile, ITestAlgorithm2.class);

        ITestAlgorithm2 a = testHelper.getInstance();
        int result = a.modification(param);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test1() {
        okRows(new File("test/rules/algorithm/Test_Factorial.xls"), 4, 24);
        okRows(new File("test/rules/algorithm/Test_Factorial.xls"), -123, 0);
        okRows(new File("test/rules/algorithm/Test_Factorial.xls"), 6, 720);
        okRows(new File("test/rules/algorithm/Test_Factorial.xls"), 0, 1);
    }

    @Test
    public void test2() {
        okRows(new File("test/rules/algorithm/Test_IsSquare.xls"), 625, 0);
        okRows(new File("test/rules/algorithm/Test_IsSquare.xls"), -123, 0);
        okRows(new File("test/rules/algorithm/Test_IsSquare.xls"), 6, 0);
    }

    @Test
    public void test3() {
        okRows(new File("test/rules/algorithm/Test_GetMaxPrime.xls"), -1, 0);
        okRows(new File("test/rules/algorithm/Test_GetMaxPrime.xls"), 10, 7);
        okRows(new File("test/rules/algorithm/Test_GetMaxPrime.xls"), 100, 97);
        okRows(new File("test/rules/algorithm/Test_GetMaxPrime.xls"), 2500, 2477);
    }
}
