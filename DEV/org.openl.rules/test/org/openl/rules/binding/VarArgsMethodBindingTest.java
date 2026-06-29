package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class VarArgsMethodBindingTest {
    private static final String SRC = "test/rules/binding/VarArgsMethodBindingTest.xlsx";

    private static TestInterf instance;

    @BeforeAll
    static void init() {
        instance = TestUtils.create(SRC, TestInterf.class);
    }

    @Test
    void test1Argument1() {
        assertTrue(instance.test1Argument());
    }

    @Test
    void test2Arguments() {
        assertTrue(instance.test2Arguments());
    }

    @Test
    void test3Arguments() {
        assertEquals(11, instance.test3Arguments());
    }

    @Test
    void test4Arguments() {
        assertEquals(5, instance.test4Arguments());
    }

    @Test
    void test5Arguments() {
        assertEquals(11, instance.test5Arguments());
    }

    @Test
    void testSummary() {
        assertEquals(10, instance.testArrayOfArrays());
    }

    public interface TestInterf {
        boolean test1Argument();

        boolean test2Arguments();

        int test3Arguments();

        int test4Arguments();

        int test5Arguments();

        int testArrayOfArrays();
    }

}
