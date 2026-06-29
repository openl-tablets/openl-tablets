package org.openl.rules.tbasic;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class TestVar {
    @Test
    void test1() {
        TestUtils.assertEx("test/rules/tbasic1/VAR_F1.xls", "Operation must have value in Condition.");
    }

    @Test
    void test2() {
        TestUtils.assertEx("test/rules/tbasic1/VAR_F2.xls", "Operation VAR cannot be multiline.");
    }

    @Test
    void test3() {
        TestUtils.create("test/rules/tbasic1/VAR_P1.xls", ITestAlgorithm1.class).modification();
    }
}
