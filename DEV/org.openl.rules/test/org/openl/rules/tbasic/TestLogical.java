package org.openl.rules.tbasic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestLogical {
    private void okRows(String xlsFile, int param, int expectedResult) {

        ITestAlgorithm2 a = TestUtils.create(xlsFile, ITestAlgorithm2.class);
        int result = a.modification(param);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test1() {
        okRows("test/rules/algorithm/Test_Factorial.xls", 4, 24);
        okRows("test/rules/algorithm/Test_Factorial.xls", -123, 0);
        okRows("test/rules/algorithm/Test_Factorial.xls", 6, 720);
        okRows("test/rules/algorithm/Test_Factorial.xls", 0, 1);
    }

    @Test
    public void test2() {
        okRows("test/rules/algorithm/Test_IsSquare.xls", 625, 0);
        okRows("test/rules/algorithm/Test_IsSquare.xls", -123, 0);
        okRows("test/rules/algorithm/Test_IsSquare.xls", 6, 0);
    }

    @Test
    public void test3() {
        okRows("test/rules/algorithm/Test_GetMaxPrime.xls", -1, 0);
        okRows("test/rules/algorithm/Test_GetMaxPrime.xls", 10, 7);
        okRows("test/rules/algorithm/Test_GetMaxPrime.xls", 100, 97);
        okRows("test/rules/algorithm/Test_GetMaxPrime.xls", 2500, 2477);
    }
}
