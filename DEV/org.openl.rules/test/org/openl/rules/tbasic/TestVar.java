package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestVar {
    @Test
    public void test1() {
        TestUtils.assertEx("test/rules/tbasic1/VAR_F1.xls", "Operation must have value in Condition!");
    }

    @Test
    public void test2() {
        TestUtils.assertEx("test/rules/tbasic1/VAR_F2.xls", "Operation VAR can not be multiline!");
    }

    @Test
    public void test3() {
        TestUtils.create("test/rules/tbasic1/VAR_P1.xls", ITestAlgorithm1.class).modification();
    }
}
