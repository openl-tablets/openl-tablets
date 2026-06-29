package org.openl.rules.tbasic;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class Test1 {
    @Test
    void test1() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-1.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test2() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-2.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test3() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-3.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test4() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-4.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test5() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm1-5.xls", "Insufficient rows. Must be more than 2.");
    }

    @Test
    void test6() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-6.xls", ITestAlgorithm1.class).modification();
    }
}
