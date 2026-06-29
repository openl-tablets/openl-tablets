package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class FieldAccessTest {

    private static final String SRC = "test/rules/binding/FieldAccessTest.xlsx";

    private static FieldAccessInterface instance;

    @BeforeAll
    static void init() {
        instance = TestUtils.create(SRC, FieldAccessInterface.class);
    }

    @Test
    void test() {
        assertEquals(100, instance.test().intValue());
    }

    @Test
    void test1() {
        assertEquals("John", instance.test1());
    }

    public interface FieldAccessInterface {
        Double test();

        String test1();
    }

}
