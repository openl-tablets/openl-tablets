package org.openl.rules.cmatch;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;

public class Test2 {
    public interface ITest2 {
        int runColumnMatch(int i1, int i2);
    }

    public interface ITest5 {
        String runColumnMatch(int i1, int i2, int i3, int i4, int i5);
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/cmatch1/match2-1.xls");
        TestHelper<ITest2> testHelper;
        testHelper = new TestHelper<ITest2>(xlsFile, ITest2.class);

        ITest2 test = testHelper.getInstance();

        assertEquals(0, test.runColumnMatch(0, 0));
        assertEquals(0, test.runColumnMatch(0, 1));

        assertEquals(6, test.runColumnMatch(2, 1));
        assertEquals(3, test.runColumnMatch(10, 1));

        assertEquals(4, test.runColumnMatch(1, 1));
        assertEquals(4, test.runColumnMatch(1, 2));

        assertEquals(5, test.runColumnMatch(10, 2));
    }

    @Test
    public void test2() {
        File xlsFile = new File("test/rules/cmatch1/match2-2.xls");
        TestHelper<ITest2> testHelper;
        testHelper = new TestHelper<ITest2>(xlsFile, ITest2.class);

        ITest2 test = testHelper.getInstance();

        assertEquals(0, test.runColumnMatch(0, 0));
        assertEquals(1, test.runColumnMatch(0, 1));
        assertEquals(2, test.runColumnMatch(0, 2));

        assertEquals(6, test.runColumnMatch(3, 3));
        assertEquals(7, test.runColumnMatch(3, 4));
        assertEquals(8, test.runColumnMatch(3, 5));

        assertEquals(3, test.runColumnMatch(3, -1));
        assertEquals(4, test.runColumnMatch(4, -1));
        assertEquals(5, test.runColumnMatch(5, -1));
    }

    @Test
    public void test3() {
        File xlsFile = new File("test/rules/cmatch1/match2-3.xls");
        TestHelper<ITest5> testHelper;
        testHelper = new TestHelper<ITest5>(xlsFile, ITest5.class);

        ITest5 test = testHelper.getInstance();

        assertEquals(null, test.runColumnMatch(0, -1, -1, -1, -1));
        assertEquals("0", test.runColumnMatch(0, -1, -1, -1, 0));
        assertEquals(null, test.runColumnMatch(0, 0, -1, -1, -1));
        assertEquals(null, test.runColumnMatch(0, 0, 0, -1, -1));
        assertEquals("2", test.runColumnMatch(0, 0, 0, -1, 2));
        assertEquals("3", test.runColumnMatch(0, 0, 0, 0, -1));

        assertEquals("4", test.runColumnMatch(1, 1, 1, 10, -1));
        assertEquals("4", test.runColumnMatch(1, 1, 1, 11, 4));
        assertEquals("7", test.runColumnMatch(1, 2, 4, 40, 4));
        assertEquals("7", test.runColumnMatch(1, 1, 1, 0, 7));
    }

    @Test
    public void test4() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match2-4.xls");
                TestHelper<ITest5> testHelper;
                testHelper = new TestHelper<ITest5>(xlsFile, ITest5.class);
            }
        }, "All sub nodes must be leaves!", "cell=B7");
    }

    @Test
    public void test5() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match2-5.xls");
                TestHelper<ITest5> testHelper;
                testHelper = new TestHelper<ITest5>(xlsFile, ITest5.class);
            }
        }, "All sub nodes must be leaves!", "cell=B7");
    }

    @Test
    public void test6() {
        TestUtils.assertEx(new Runnable() {
            public void run() {
                File xlsFile = new File("test/rules/cmatch1/match2-6.xls");
                TestHelper<ITest5> testHelper;
                testHelper = new TestHelper<ITest5>(xlsFile, ITest5.class);
            }
        }, "Illegal indent!", "cell=B10");
    }
}
