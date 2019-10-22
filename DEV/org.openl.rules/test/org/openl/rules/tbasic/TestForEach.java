package org.openl.rules.tbasic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.TestUtils;

/**
 * Created by dl on 9/10/14.
 */
public class TestForEach {
    @Test
    public void test1() {
        TestUtils.assertEx(("test/rules/tbasic1/FOR_EACH_F1.xls"),
            "Compilation failure. The cell should be of the array type");
    }

    @Test
    public void test2() {
        TestUtils.assertEx(("test/rules/tbasic1/FOR_EACH_F2.xls"), "Operation must have value in Condition.");
    }

    @Test
    public void test3() {
        TestUtils.assertEx(("test/rules/tbasic1/FOR_EACH_F3.xls"), "Operation FOR EACH cannot be singleline.");
    }

    @Test
    public void test4() {
        TestUtils.assertEx(("test/rules/tbasic1/FOR_EACH_F4.xls"), "Operation must have value in Action.");
    }

    @Test
    public void test5() {
        TestUtils.assertEx(("test/rules/tbasic1/FOR_EACH_F5.xls"),
            "Compilation failure. The cell should be of the array type");
    }

    @Test
    public void test6() {
        TestUtils.assertEx(("test/rules/tbasic1/FOR_EACH_F6.xls"),
            "Compilation failure. The cell should be of the array type");
    }

    @Test
    public void test7() {
        TestUtils.assertEx(("test/rules/tbasic1/FOR_EACH_F7.xls"), "Variable 'el' has already been defined");
    }

    @Test
    public void test8() {
        TestAlgorithm a = TestUtils.create("test/rules/tbasic1/FOR_EACH_P1.xls", TestAlgorithm.class);

        Integer result = a.calc();
        assertEquals(0, result.intValue());
    }

    @Test
    public void test8_1() {
        TestUtils.create("test/rules/tbasic1/FOR_EACH_P2.xls", ITestAlgorithm1.class).modification();
    }

    @Test
    public void test9() {
        TestAlgorithm a = TestUtils.create("test/rules/tbasic1/FOR_EACH_P3.xls", TestAlgorithm.class);

        Integer result = a.calc();
        assertEquals(36, result.intValue());
    }

    @Test
    public void test10() {
        TestAlgorithm a = TestUtils.create("test/rules/tbasic1/FOR_EACH_P4.xls", TestAlgorithm.class);

        Integer result = a.calc();
        assertEquals(114, result.intValue());
    }

    @Test
    public void test11() {
        TestAlgorithm a = TestUtils.create("test/rules/tbasic1/FOR_EACH_P5.xls", TestAlgorithm.class);

        Integer result = a.calc();
        assertEquals(3, result.intValue());
    }

    public interface TestAlgorithm {
        Integer calc();
    }
}
