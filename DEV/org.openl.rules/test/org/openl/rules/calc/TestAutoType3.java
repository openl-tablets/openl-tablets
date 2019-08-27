package org.openl.rules.calc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.TestUtils;

public class TestAutoType3 {

    private static String csr;

    @BeforeClass
    public static void before() {
        csr = System.getProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "");
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "false");
    }

    @AfterClass
    public static void after() {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, csr);
    }

    @Test
    public void test1() {
        TestUtils.assertEx("test/rules/calc/autotype/autotype-3.xls",
            "Spreadsheet Expression Loop: [R1C1, R1C3]");
    }
}
