package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.TestHelper;

public class ObscuringTest {
    private static final String SRC = "test/rules/binding/ObscuringTest.xls";

    private static TestInterf instance;

    public interface TestInterf {
        Double localMultiply();

        Double testParameterMultiply();
    }

    @Before
    public void init() {
        if (instance == null) {
            instance = new TestHelper<TestInterf>(new File(SRC), TestInterf.class).getInstance();
        }
    }

    @Test
    public void test3Arguments() {
        assertEquals("7.5", "" + instance.localMultiply());
    }

    @Test
    public void testSummary() {
        assertEquals("16.5", "" + instance.testParameterMultiply());
    }

}
