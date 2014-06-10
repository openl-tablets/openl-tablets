package org.openl.rules.datatype;

import org.junit.Test;
import org.openl.rules.TestHelper;

import java.io.File;

public class WrongAliasDefaultValuesTest {

    @Test(expected = Exception.class)
    public void test1() {
        testFile("test/rules/datatype/WrongAliasDefaultValues1.xls");
    }

    @Test(expected = Exception.class)
    public void test2() {
        testFile("test/rules/datatype/WrongAliasDefaultValues2.xls");
    }

    @Test(expected = Exception.class)
    public void test3() {
        testFile("test/rules/datatype/WrongAliasDefaultValues3.xls");
    }

    @Test(expected = Exception.class)
    public void test4() {
        testFile("test/rules/datatype/WrongAliasDefaultValues4.xls");
    }

    private void testFile(String pathname) {
        File xlsFile = new File(pathname);
        TestHelper<ITest> testHelper = new TestHelper<ITest>(xlsFile, ITest.class);
        testHelper.getInstance();
    }

    private interface ITest {
    }
}
