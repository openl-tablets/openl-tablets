package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class ObscuringTest {
    private static final String SRC = "test/rules/binding/ObscuringTest.xls";

    private static TestInterf instance;

    @BeforeAll
    static void init() {
        instance = TestUtils.create(SRC, TestInterf.class);
    }

    @Test
    void test3Arguments() {
        assertEquals("7.5", "" + instance.localMultiply());
    }

    @Test
    void testSummary() {
        assertEquals("16.5", "" + instance.testParameterMultiply());
    }

    public interface TestInterf {
        Double localMultiply();

        Double testParameterMultiply();
    }

}
