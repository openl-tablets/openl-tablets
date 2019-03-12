package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestUtils;

public class FieldAccessTest {

    private static final String SRC = "test/rules/binding/FieldAccessTest.xlsx";

    private static FieldAccessInterface instance;

    @BeforeClass
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
        DoubleValue test();

        String test1();
    }

}
