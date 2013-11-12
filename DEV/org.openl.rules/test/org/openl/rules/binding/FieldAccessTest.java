package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.TestHelper;

public class FieldAccessTest {

    private static final String SRC = "test/rules/binding/FieldAccessTest.xlsx";

    private static FieldAccessInterface instance;

    public interface FieldAccessInterface {
        DoubleValue test();

        String test1();
    }

    @BeforeClass
    public static void init() {
        File xlsFile = new File(SRC);
        TestHelper<FieldAccessInterface> testHelper = new TestHelper<FieldAccessInterface>(xlsFile,
                FieldAccessInterface.class);
        instance = testHelper.getInstance();
    }

    @Test
    public void test() {
        assertEquals(100, instance.test().intValue());
    }

    @Test
    public void test1() {
        assertEquals("John", instance.test1());
    }

}
