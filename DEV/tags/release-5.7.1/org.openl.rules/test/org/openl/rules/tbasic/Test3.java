package org.openl.rules.tbasic;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.syntax.exception.CompositeSyntaxNodeException;

public class Test3 extends Test0 {
    @Test
    public void test0() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Algorithm3-0.xls"));
        TestUtils.assertEx(ex, "Unsufficient rows. Must be more than 2!");
    }

    @Test
    public void test1() {
        okRows(new File("test/rules/tbasic0/Algorithm3-1.xls"), 0);
    }

    @Test
    public void test2() {
        okRows(new File("test/rules/tbasic0/Algorithm3-2.xls"), 0);
    }

    @Test
    public void test3() {
        okRows(new File("test/rules/tbasic0/Algorithm3-3.xls"), 0);
    }

    @Test
    public void test4() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Algorithm3-4.xls"));
        assertTrue(ex instanceof CompositeSyntaxNodeException);
//        TestUtils.assertEx(ex, "org.openl.syntax.SyntaxErrorException:");
    }

    @Test
    public void test5() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Algorithm3-4.xls"));
        assertTrue(ex instanceof CompositeSyntaxNodeException);
//        TestUtils.assertEx(ex, "org.openl.syntax.SyntaxErrorException:");
    }

    @Test
    public void test6() {
        okRows(new File("test/rules/tbasic0/Algorithm3-6.xls"), 0);
    }
    
    @Test
    public void testDuplicateColumn() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Test_Duplicate_Column_In_TBasic.xls"));
        TestUtils.assertEx(ex, "Duplicate column");
    }

    @Test
    public void test7() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Algorithm3-7.xls"));
        TestUtils.assertEx(ex, "Invalid column id");
    }

    @Test
    public void test8() {
        okRows(new File("test/rules/tbasic0/Algorithm3-8.xls"), 0);
    }

    @Test
    public void test9() {
        okRows(new File("test/rules/tbasic0/Algorithm3-9.xls"), 0);
    }
}
