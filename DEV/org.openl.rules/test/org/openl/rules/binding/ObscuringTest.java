package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class ObscuringTest {
    private static final String SRC = "test/rules/binding/ObscuringTest.xls";

    private static TestInterf instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC, TestInterf.class);
    }

    @Test
    public void test3Arguments() {
        assertEquals("7.5", "" + instance.localMultiply());
    }

    @Test
    public void testSummary() {
        assertEquals("16.5", "" + instance.testParameterMultiply());
    }

    public interface TestInterf {
        Double localMultiply();

        Double testParameterMultiply();
    }

}
