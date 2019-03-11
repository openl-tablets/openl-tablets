package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class ArrayLoadTest {

    @Test
    public void testMultiRowArrayLoad() {
        ITestI instance = TestUtils.create("test/rules/dt/MultiRowArrayLoadTest.xls", ITestI.class);

        String s = instance.hello1(0);
        assertEquals("Good night", s);

        s = instance.hello1(6);
        assertEquals("Good morning", s);
    }

    public interface ITestI {
        String hello1(int hour);
    }
}
