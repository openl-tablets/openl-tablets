package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestSet {
    @Test
    public void test1() {
        TestUtils.assertEx("test/rules/tbasic1/SET_F1.xls", "Operation must not have value in Condition!");
    }

    @Test
    public void test2() {
        TestUtils.assertEx("test/rules/tbasic1/SET_F2.xls", "Operation must not have value in Condition!");
    }

    @Test
    public void test3() {
        TestUtils.assertEx("test/rules/tbasic1/SET_F3.xls", "Operation must have value in Action!");
    }

    @Test
    public void test4() {
        TestUtils.assertEx("test/rules/tbasic1/SET_F4.xls", "Operation SET can not be multiline!");
    }

    @Test
    public void test5() {
        TestUtils.create("test/rules/tbasic1/SET_P1.xls", ITestAlgorithm1.class).modification();
    }
}
