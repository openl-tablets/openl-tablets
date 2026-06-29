package org.openl.rules.tbasic;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class Test2 {
    @Test
    void test1() {
        TestUtils.create("test/rules/tbasic0/Algorithm2-1.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test2() {
        TestUtils.create("test/rules/tbasic0/Algorithm2-2.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test3() {
        TestUtils.create("test/rules/tbasic0/Algorithm2-3.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test4() {
        TestUtils.create("test/rules/tbasic0/Algorithm2-4.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    void test5() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm2-5.xls", "Duplicate column");
    }

    @Test
    void test6() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm2-6.xls", "Invalid column id");
    }
}
