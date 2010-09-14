package org.openl.rules.helpers;

import junit.framework.TestCase;

public class TablePrinterTest extends TestCase {

    String[][] x = { { "AA", "BB", "CCC" }, { "DDDDDDDDDDDDDDD" }, { "UUU", "FF" } };

    public void test1() {
        System.out.println(new TablePrinter(x, null, " ! ").print());
    }
}
