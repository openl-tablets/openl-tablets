package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestIf {
    @Test
    public void test1() {
        TestUtils.assertEx("test/rules/tbasic1/IF_F1.xls", "Operation must not have value in Action!");
    }

    @Test
    public void test2() {
        TestUtils.assertEx("test/rules/tbasic1/IF_F2.xls", "Operation must have value in Action!");
    }

    @Test
    public void test3() {
        TestUtils.assertEx("test/rules/tbasic1/IF_F3.xls", "Operation must have value in Condition!");
    }

    @Test
    public void test4() {
        TestUtils.assertEx("test/rules/tbasic1/IF_F4.xls", "Operation must have value in Condition!");
    }

    @Test
    public void test5() {
        TestUtils.assertEx("test/rules/tbasic1/IF_F5.xls", "Operation must have value in Condition!");
    }

    @Test
    public void test6() {
        TestUtils.create("test/rules/tbasic1/IF_P1.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test7() {
        TestUtils.create("test/rules/tbasic1/IF_P2.xls", ITestAlgorithm1.class).modification();
    }
}
