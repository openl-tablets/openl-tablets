package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.TestUtils;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Created by dl on 9/10/14.
 */
public class TestForEach extends Test0 {
    @Test
    public void test1() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F1.xls"));
        TestUtils.assertEx(ex, "Compilation failure. The cell should be of the array type");
    }

    @Test
    public void test2() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F2.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Test
    public void test3() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F3.xls"));
        TestUtils.assertEx(ex, "Operation FOR EACH can not be singleline!");
    }

    @Test
    public void test4() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F4.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Action!");
    }

    @Test
    public void test5() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F5.xls"));
        TestUtils.assertEx(ex, "Compilation failure. The cell should be of the array type");
    }

    @Test
    public void test6() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F6.xls"));
        TestUtils.assertEx(ex, "Compilation failure. The cell should be of the array type");
    }

    @Test
    public void test7() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F7.xls"));
        TestUtils.assertEx(ex, "Variable 'el' has already been defined");
    }

    @Test
    public void test8() {
        TestHelper<TestAlgorithm> testHelper = new TestHelper<TestAlgorithm>(new File("test/rules/tbasic1/FOR_EACH_P1.xls"), TestAlgorithm.class);
        TestAlgorithm a = testHelper.getInstance();

        Integer result = a.calc();
        assertEquals(0, result.intValue());
    }

    @Test
    public void test8_1() {
        okRows(new File("test/rules/tbasic1/FOR_EACH_P2.xls"), 1);
    }

    @Test
    public void test9() {
        TestHelper<TestAlgorithm> testHelper = new TestHelper<TestAlgorithm>(new File("test/rules/tbasic1/FOR_EACH_P3.xls"), TestAlgorithm.class);
        TestAlgorithm a = testHelper.getInstance();

        Integer result = a.calc();
        assertEquals(36, result.intValue());
    }

    @Test
    public void test10() {
        TestHelper<TestAlgorithm> testHelper = new TestHelper<TestAlgorithm>(new File("test/rules/tbasic1/FOR_EACH_P4.xls"), TestAlgorithm.class);
        TestAlgorithm a = testHelper.getInstance();

        Integer result = a.calc();
        assertEquals(114, result.intValue());
    }

    @Test
    public void test11() {
        TestHelper<TestAlgorithm> testHelper = new TestHelper<TestAlgorithm>(new File("test/rules/tbasic1/FOR_EACH_P5.xls"), TestAlgorithm.class);
        TestAlgorithm a = testHelper.getInstance();

        Integer result = a.calc();
        assertEquals(3, result.intValue());
    }

    public interface TestAlgorithm {
        Integer calc();
    }

    public interface TestAlgorithm1 {
        Integer calc(List<Integer> list);
    }

}
