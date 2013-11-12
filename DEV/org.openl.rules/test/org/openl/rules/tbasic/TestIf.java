package org.openl.rules.tbasic;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class TestIf extends Test0 {
    @Test
    public void test1() {
        Exception ex = catchEx(new File("test/rules/tbasic1/IF_F1.xls"));
        TestUtils.assertEx(ex, "Operation must not have value in Action!");
    }

    @Test
    public void test2() {
        Exception ex = catchEx(new File("test/rules/tbasic1/IF_F2.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Action!");
    }

    @Test
    public void test3() {
        Exception ex = catchEx(new File("test/rules/tbasic1/IF_F3.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Test
    public void test4() {
        Exception ex = catchEx(new File("test/rules/tbasic1/IF_F4.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Test
    public void test5() {
        Exception ex = catchEx(new File("test/rules/tbasic1/IF_F5.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Test
    public void test6() {
        okRows(new File("test/rules/tbasic1/IF_P1.xls"), 1);
    }

    @Test
    public void test7() {
        okRows(new File("test/rules/tbasic1/IF_P2.xls"), 1);
    }
}
