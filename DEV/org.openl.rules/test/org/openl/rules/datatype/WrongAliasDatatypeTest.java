package org.openl.rules.datatype;

import java.io.File;

import org.junit.Test;
import org.openl.rules.TestHelper;

public class WrongAliasDatatypeTest {

    public interface ITest {
    }

    // String
    @Test
    public void test1() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatype1Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test2() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatype2Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test3() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatype3Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test4() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatype4Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test6() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatypeUsage2Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test7() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatypeUsage3Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test8() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatypeUsage4Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test9() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatypeUsage5Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }

    @Test(expected = Exception.class)
    public void test10() {
        File xlsFile = new File("test/rules/datatype/WrongAliasDatatypeUsage6Test.xlsx");
        TestHelper<ITest> testHelper = new TestHelper<>(xlsFile, ITest.class);

        testHelper.getInstance();
    }
}
