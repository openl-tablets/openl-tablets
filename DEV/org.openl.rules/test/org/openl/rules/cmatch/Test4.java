package org.openl.rules.cmatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class Test4 {
    @Test
    void test1() {

        ITest4 test = TestUtils.create("test/rules/cmatch1/match4-1.xls", ITest4.class);

        assertEquals(101, test.runColumnMatch(10, 0, 0, 0));
        assertEquals(51, test.runColumnMatch(0, 5, 0, 0));
        assertEquals(180, test.runColumnMatch(10, 5, 4, 3));
        assertEquals(91, test.runColumnMatch(4, 3, 3, 2));
        assertEquals(20, test.runColumnMatch(1, 1, 1, 1));
        assertEquals(0, test.runColumnMatch(-1, -1, -1, -1));
    }

    @Test
    void test2() {
        TestUtils.assertEx("test/rules/cmatch1/match4-2.xls", "Sub node are prohibited here.", "cell=B8");
    }

    @Test
    void test3() {
        TestUtils.assertEx("test/rules/cmatch1/match4-3.xls", "Cannot convert an empty string to a number.", "cell=F6");
    }

    @Test
    void test4() {
        TestUtils.assertEx("test/rules/cmatch1/match4-4.xls",
                "Score algorithm supports int or Integer return type only.",
                "range=B3:M10");
    }

    public interface ITest4 {
        int runColumnMatch(int i1, int i2, int i3, int i4);
    }
}
