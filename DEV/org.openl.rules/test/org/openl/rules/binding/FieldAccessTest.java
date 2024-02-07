package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

public class FieldAccessTest {

    private static final String SRC = "test/rules/binding/FieldAccessTest.xlsx";

    private static FieldAccessInterface instance;

    @BeforeAll
    public static void init() {
        instance = TestUtils.create(SRC, FieldAccessInterface.class);
    }

    @Test
    public void test() {
        assertEquals(100, instance.test().intValue());
    }

    @Test
    public void test1() {
        assertEquals("John", instance.test1());
    }

    public interface FieldAccessInterface {
        Double test();

        String test1();
    }

}
