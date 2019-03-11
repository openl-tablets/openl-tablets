package org.openl.rules.datatype;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class WrongAliasDatatypeTest {

    @Test
    public void test1() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatype1Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test2() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatype2Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test3() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatype3Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test4() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatype4Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test6() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage2Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test7() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage3Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test8() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage4Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test9() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage5Test.xlsx");
    }

    @Test(expected = Exception.class)
    public void test10() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage6Test.xlsx");
    }
}
