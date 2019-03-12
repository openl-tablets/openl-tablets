package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class ArrayLoadInSingleCellTest {

    @Test
    public void testMultiRowArrayLoad() {
        ITestI instance = TestUtils.create("test/rules/dt/SingleCellArrayLoadTest.xls", ITestI.class);

        String s = instance.test1("d1", 0);
        assertEquals("d1-1", s);

        s = instance.test1("d2", 2);
        assertEquals("d2-3", s);
        s = instance.test1("d3", 1);
        assertEquals("d3-2", s);
        s = instance.test1("d4", 2);
        assertEquals("d4-3", s);
    }

    public interface ITestI {
        String test1(String code, int idx);
    }

}
