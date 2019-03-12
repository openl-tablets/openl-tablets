package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test3 {
    @Test
    public void test0() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm3-0.xls", "Unsufficient rows. Must be more than 2!");
    }

    @Test
    public void test1() {
        TestUtils.create("test/rules/tbasic0/Algorithm3-1.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test2() {
        TestUtils.create("test/rules/tbasic0/Algorithm3-2.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test3() {
        TestUtils.create("test/rules/tbasic0/Algorithm3-3.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test4() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm3-4.xls", "Operation must have value in Condition!");
    }

    @Test
    public void test5() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm3-4.xls", "Operation must have value in Condition!");
    }

    @Test
    public void test6() {
        TestUtils.create("test/rules/tbasic0/Algorithm3-6.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void testDuplicateColumn() {
        TestUtils.assertEx("test/rules/tbasic0/Test_Duplicate_Column_In_TBasic.xls", "Duplicate column");
    }

    @Test
    public void test7() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm3-7.xls", "Invalid column id");
    }

    @Test
    public void test8() {
        TestUtils.create("test/rules/tbasic0/Algorithm3-8.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test9() {
        TestUtils.create("test/rules/tbasic0/Algorithm3-9.xls", ITestAlgorithm1.class).modification();
    }
}
