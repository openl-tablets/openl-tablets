package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test1 {
    @Test
    public void test1() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-1.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test2() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-2.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test3() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-3.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test4() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-4.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test5() {
        TestUtils.assertEx("test/rules/tbasic0/Algorithm1-5.xls", "Unsufficient rows. Must be more than 2!");
    }

    @Test
    public void test6() {
        TestUtils.create("test/rules/tbasic0/Algorithm1-6.xls", ITestAlgorithm1.class).modification();
    }
}
