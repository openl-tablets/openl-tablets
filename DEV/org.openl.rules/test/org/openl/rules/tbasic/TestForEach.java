package org.openl.rules.tbasic;

import org.junit.Ignore;
import org.junit.Test;
import org.openl.rules.TestUtils;

import java.io.File;

/**
 * Created by dl on 9/10/14.
 */
public class TestForEach extends Test0 {
    @Test
    public void test1() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F1.xls"));
        TestUtils.assertEx(ex, "Operation must not have value in Action!");
    }

    @Test
    public void test2() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F2.xls"));
        TestUtils.assertEx(ex, "Operation must not have value in Action!");
    }

    @Test
    public void test3() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F3.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Action!");
    }

    @Test
    public void test4() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F4.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Test
    public void test5() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_F5.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Ignore
    @Test
    public void test6() {
        Exception ex = catchEx(new File("test/rules/tbasic1/FOR_EACH_P1.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }
}
