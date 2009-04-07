package org.openl.rules.tbasic;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class Test1 extends Test0 {
    @Test
    public void test1() {
        okRows(new File("test/rules/tbasic0/Algorithm1-1.xls"), 1);
    }

    @Test
    public void test2() {
        okRows(new File("test/rules/tbasic0/Algorithm1-2.xls"), 1);
    }

    @Test
    public void test3() {
        okRows(new File("test/rules/tbasic0/Algorithm1-3.xls"), 1);
    }

    @Test
    public void test4() {
        okRows(new File("test/rules/tbasic0/Algorithm1-4.xls"), 1);
    }

    @Test
    public void test5() {
        Exception ex = catchEx(new File("test/rules/tbasic0/Algorithm1-5.xls"));
        TestUtils.assertEx(ex, "Unsufficient rows. Must be more than 2!");
    }

    @Test
    public void test6() {
        okRows(new File("test/rules/tbasic0/Algorithm1-6.xls"), 1);
    }
}
