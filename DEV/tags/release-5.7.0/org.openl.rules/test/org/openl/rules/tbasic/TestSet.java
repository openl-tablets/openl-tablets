package org.openl.rules.tbasic;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.syntax.exception.CompositeSyntaxNodeException;

public class TestSet extends Test0 {
    @Test
    public void test1() {
        Exception ex = catchEx(new File("test/rules/tbasic1/SET_F1.xls"));
        TestUtils.assertEx(ex, "Operation must not have value in Condition!");
    }

    @Test
    public void test2() {
        Exception ex = catchEx(new File("test/rules/tbasic1/SET_F2.xls"));
        assertTrue(ex instanceof CompositeSyntaxNodeException);
//        TestUtils.assertEx(ex, "org.openl.syntax.SyntaxErrorException:");
    }

    @Test
    public void test3() {
        Exception ex = catchEx(new File("test/rules/tbasic1/SET_F3.xls"));
        TestUtils.assertEx(ex, "Operation must have value in Action!");
    }

    @Test
    public void test4() {
        Exception ex = catchEx(new File("test/rules/tbasic1/SET_F4.xls"));
        TestUtils.assertEx(ex, "Operation SET can not be multiline!");
    }

    @Test
    public void test5() {
        okRows(new File("test/rules/tbasic1/SET_P1.xls"), 0);
    }
}
