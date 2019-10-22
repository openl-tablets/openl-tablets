package org.openl.rules.cmatch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test3 {
    @Test
    public void test1() {
        ITest4 test = TestUtils.create("test/rules/cmatch1/match3-1.xls", ITest4.class);

        assertEquals("AAA", test.runColumnMatch(10, 0, 0, 0));
        assertEquals("B", test.runColumnMatch(0, 5, 0, 0));
        assertEquals("AAA", test.runColumnMatch(10, 5, 4, 3));
        assertEquals("AA", test.runColumnMatch(4, 3, 3, 2));
        assertEquals("D", test.runColumnMatch(1, 1, 1, 1));
        assertEquals("D", test.runColumnMatch(1, 1, 1, 0));
    }

    @Test
    public void test2() {
        TestUtils.assertEx("test/rules/cmatch1/match3-2.xls", "Sub node are prohibited here.", "cell=B10");
    }

    @Test
    public void test3() {
        TestUtils.assertEx("test/rules/cmatch1/match3-3.xls",
            "Column operation of special row Total Score must be defined.",
            "cell=C7");
    }

    public interface ITest4 {
        String runColumnMatch(int i1, int i2, int i3, int i4);
    }
}
