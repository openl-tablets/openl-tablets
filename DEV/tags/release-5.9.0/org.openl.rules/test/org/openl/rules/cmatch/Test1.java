package org.openl.rules.cmatch;

import static junit.framework.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.cmatch.test.TestEnum;
import org.openl.rules.cmatch.test.TestObj;

public class Test1 {
    public interface ITestD {
        long runColumnMatch(double d);
    }

    public interface ITestE {
        String runColumnMatch(TestEnum e);
    }

    public interface ITestI {
        int runColumnMatch(int i);
    }

    public interface ITestO {
        String runColumnMatch(TestObj obj);
    }

    public interface ITestS {
        int runColumnMatch(String s);
    }

    @Test
    public void testD() {
        File xlsFile = new File("test/rules/cmatch1/match1-d.xls");
        TestHelper<ITestD> testHelper;
        testHelper = new TestHelper<ITestD>(xlsFile, ITestD.class);

        ITestD test = testHelper.getInstance();

        assertEquals(1, test.runColumnMatch(1.0));
        assertEquals(1, test.runColumnMatch(1.1));
        assertEquals(2, test.runColumnMatch(2.2));
        assertEquals(2, test.runColumnMatch(2.3));
        assertEquals(2, test.runColumnMatch(2.4));
        assertEquals(3, test.runColumnMatch(2.7));
    }

    @Test
    public void testE() {
        File xlsFile = new File("test/rules/cmatch1/match1-e.xls");
        TestHelper<ITestE> testHelper;
        testHelper = new TestHelper<ITestE>(xlsFile, ITestE.class);

        ITestE test = testHelper.getInstance();

        assertEquals("8", test.runColumnMatch(TestEnum.EIGHT));
        assertEquals("5", test.runColumnMatch(TestEnum.FIVE));
        assertEquals("1", test.runColumnMatch(TestEnum.ONE));
        assertEquals(null, test.runColumnMatch(TestEnum.SEVEN));
    }

    @Test
    public void testI() {
        File xlsFile = new File("test/rules/cmatch1/match1-i.xls");
        TestHelper<ITestI> testHelper;
        testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI test = testHelper.getInstance();

        assertEquals(1, test.runColumnMatch(-10));
        assertEquals(1, test.runColumnMatch(0));
        assertEquals(2, test.runColumnMatch(1));
        assertEquals(4, test.runColumnMatch(2));

        assertEquals(8, test.runColumnMatch(3));
        assertEquals(16, test.runColumnMatch(4));
        assertEquals(32, test.runColumnMatch(5));
    }

    @Test
    public void testO() {
        File xlsFile = new File("test/rules/cmatch1/match1-o.xls");
        TestHelper<ITestO> testHelper;
        testHelper = new TestHelper<ITestO>(xlsFile, ITestO.class);

        ITestO test = testHelper.getInstance();

        assertEquals("Low", test.runColumnMatch(new TestObj("High", 0)));
        assertEquals("Moderate", test.runColumnMatch(new TestObj("High", 4)));
        assertEquals("High", test.runColumnMatch(new TestObj("High", 5)));

        assertEquals("Very Low", test.runColumnMatch(new TestObj("Moderate", 3)));
        assertEquals("Low", test.runColumnMatch(new TestObj("Moderate", 4)));
        assertEquals("Moderate", test.runColumnMatch(new TestObj("Moderate", 10)));

        assertEquals("Low", test.runColumnMatch(new TestObj("Low", 1)));
        assertEquals("Moderate", test.runColumnMatch(new TestObj("Low", 5)));
        assertEquals("High", test.runColumnMatch(new TestObj("Low", 6)));
    }

    @Test
    public void testS() {
        File xlsFile = new File("test/rules/cmatch1/match1-s.xls");
        TestHelper<ITestS> testHelper;
        testHelper = new TestHelper<ITestS>(xlsFile, ITestS.class);

        ITestS test = testHelper.getInstance();

        assertEquals(8, test.runColumnMatch("A"));
        assertEquals(7, test.runColumnMatch("B"));
        assertEquals(6, test.runColumnMatch("C"));

        assertEquals(4, test.runColumnMatch("D"));
        assertEquals(3, test.runColumnMatch("E"));
        assertEquals(2, test.runColumnMatch("F"));
    }
}
