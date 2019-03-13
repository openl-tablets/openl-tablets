package org.openl.rules.binding;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

/**
 * Testing following constructions working in rules: DoubleValue a = new DoubleValue(-5); DoubleValue b = new
 * DoubleValue(-5); a==b; - will be true
 * 
 * @author DLiauchuk
 *
 */
public class NumbersEQTest {
    private static final String SRC = "test/rules/binding/NumbersEQTest.xls";
    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC);
    }

    @Test
    public void testDoubleValueEQ() {
        assertTrue(TestUtils.invoke(instance, "testDVEquals"));
    }

    @Test
    public void testDoubleEQ() {
        assertTrue(TestUtils.invoke(instance, "testDDEquals"));
    }
}
