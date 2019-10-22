package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestUtils;

/**
 * Created by dl on 9/9/14.
 */
public class TestWhile {
    @Test
    public void test1() {
        TestUtils.assertEx("test/rules/tbasic1/WHILE_F1.xls", "Operation must not have value in Action.");
    }

    @Test
    public void test2() {
        TestUtils.assertEx("test/rules/tbasic1/WHILE_F2.xls", "Operation must not have value in Action.");
    }

    @Test
    public void test3() {
        TestUtils.assertEx("test/rules/tbasic1/WHILE_F3.xls", "Operation must have value in Action.");
    }

    @Test
    public void test4() {
        TestUtils.assertEx("test/rules/tbasic1/WHILE_F4.xls", "Operation must have value in Condition.");
    }

    @Test
    public void test5() {
        TestUtils.assertEx("test/rules/tbasic1/WHILE_F5.xls", "Operation must have value in Condition.");
    }

    @Test
    public void test6() {
        TestUtils.create("test/rules/tbasic1/WHILE_P1.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test7() {
        TestUtils.create("test/rules/tbasic1/WHILE_P2.xls", ITestAlgorithm1.class).modification();
    }
}
