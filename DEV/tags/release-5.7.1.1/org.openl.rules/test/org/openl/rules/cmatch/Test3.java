package org.openl.rules.cmatch;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;

public class Test3 {
    public interface ITest4 {
        String runColumnMatch(int i1, int i2, int i3, int i4);
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/cmatch1/match3-1.xls");
        TestHelper<ITest4> testHelper;
        testHelper = new TestHelper<ITest4>(xlsFile, ITest4.class);

        ITest4 test = testHelper.getInstance();

        assertEquals("AAA", test.runColumnMatch(10, 0, 0, 0));
        assertEquals("B", test.runColumnMatch(0, 5, 0, 0));
        assertEquals("AAA", test.runColumnMatch(10, 5, 4, 3));
        assertEquals("AA", test.runColumnMatch(4, 3, 3, 2));
        assertEquals("D", test.runColumnMatch(1, 1, 1, 1));
        assertEquals("D", test.runColumnMatch(1, 1, 1, 0));
    }

    @Test
    public void test2() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match3-2.xls");
                TestHelper<ITest4> testHelper;
                testHelper = new TestHelper<ITest4>(xlsFile, ITest4.class);
            }
        }, "Sub node are prohibited here!", "cell=B10");
    }

    @Test
    public void test3() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match3-3.xls");
                TestHelper<ITest4> testHelper;
                testHelper = new TestHelper<ITest4>(xlsFile, ITest4.class);
            }
        }, "Column operation of special row Total Score must be defined!", "cell=C7");
    }
}
