package org.openl.rules.cmatch;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;
import org.openl.util.StringTool;

public class Test4 {
    public interface ITest4 {
        int runColumnMatch(int i1, int i2, int i3, int i4);
    }

    public interface ITest4E {
        String nonIntScore(int i1, int i2, int i3, int i4);
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/cmatch1/match4-1.xls");
        TestHelper<ITest4> testHelper = new TestHelper<ITest4>(xlsFile, ITest4.class);

        ITest4 test = testHelper.getInstance();

        assertEquals(101, test.runColumnMatch(10, 0, 0, 0));
        assertEquals(51, test.runColumnMatch(0, 5, 0, 0));
        assertEquals(180, test.runColumnMatch(10, 5, 4, 3));
        assertEquals(91, test.runColumnMatch(4, 3, 3, 2));
        assertEquals(20, test.runColumnMatch(1, 1, 1, 1));
        assertEquals(0, test.runColumnMatch(-1, -1, -1, -1));
    }

    @Test
    public void test2() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match4-2.xls");                
                new TestHelper<ITest4>(xlsFile, ITest4.class);
            }
        }, "Sub node are prohibited here!", "cell=B8");
    }

    @Test
    public void test3() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match4-3.xls");                
                new TestHelper<ITest4>(xlsFile, ITest4.class);
            }
        }, "Cannot convert an empty String to numeric type", "cell=F6");
    }

    @Test
    public void test4() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match4-4.xls");
                TestHelper<ITest4E> testHelper;
                testHelper = new TestHelper<ITest4E>(xlsFile, ITest4E.class);

                ITest4E test = testHelper.getInstance();
                test.nonIntScore(10, 0, 0, 0);
            }
        }, "Score algorithm supports int or Integer return type only!", "range=B3:M10");
    }
}
