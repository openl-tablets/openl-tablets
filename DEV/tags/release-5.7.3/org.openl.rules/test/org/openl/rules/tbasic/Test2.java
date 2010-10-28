package org.openl.rules.tbasic;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test2 extends Test0 {
    @Test
    public void test1() {
        okRows(new File("test/rules/tbasic0/Algorithm2-1.xls"), 1);
    }

    @Test
    public void test2() {
        okRows(new File("test/rules/tbasic0/Algorithm2-2.xls"), 1);
    }

    @Test
    public void test3() {
        okRows(new File("test/rules/tbasic0/Algorithm2-3.xls"), 1);
    }

    @Test
    public void test4() {
        okRows(new File("test/rules/tbasic0/Algorithm2-4.xls"), 1);
    }

    @Test
    public void test5() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Algorithm2-5.xls"));
        TestUtils.assertEx(ex, "Duplicate column");
    }

    @Test
    public void test6() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Algorithm2-6.xls"));
        TestUtils.assertEx(ex, "Invalid column id");
    }
}
