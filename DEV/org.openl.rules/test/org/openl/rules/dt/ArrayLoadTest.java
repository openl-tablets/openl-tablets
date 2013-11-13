package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class ArrayLoadTest {

    public interface ITestI {
        String hello1(int hour);
    }

    @Test
    public void testMultiRowArrayLoad() {
        File xlsFile = new File("test/rules/dt/MultiRowArrayLoadTest.xls");
        TestHelper<ITestI> testHelper = new TestHelper<ITestI>(xlsFile, ITestI.class);

        ITestI instance = testHelper.getInstance();

        String s = instance.hello1(0);
        assertEquals("Good night", s);

        s = instance.hello1(6);
        assertEquals("Good morning", s);
    }
}
