package org.openl.rules.tbasic;

import org.junit.Test;
import org.openl.rules.TestUtils;

import java.io.File;

/**
 * Created by dl on 9/9/14.
 */
public class TestWhile extends Test0 {
    @Test
    public void test1() {
        Exception ex = catchEx(new File("test/rules/tbasic1/WHILE_F1.xls"));
        TestUtils.assertEx(ex, "Operation must not have value in Action!");
    }

    @Test
    public void test2() {
        Exception ex = catchEx(new File("test/rules/tbasic1/WHILE_F2.xls"));
        TestUtils.assertEx(ex, "Operation must not have value in Action!");
    }

    @Test
    public void test3() {
        Exception ex = catchEx(new File("test/rules/tbasic1/WHILE_F3.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Action!");
    }

    @Test
    public void test4() {
        Exception ex = catchEx(new File("test/rules/tbasic1/WHILE_F4.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Test
    public void test5() {
        Exception ex = catchEx(new File("test/rules/tbasic1/WHILE_F5.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Condition!");
    }

    @Test
    public void test6() {
        okRows(new File("test/rules/tbasic1/WHILE_P1.xls"), 1);
    }

    @Test
    public void test7() {
        okRows(new File("test/rules/tbasic1/WHILE_P2.xls"), 1);
    }
}
