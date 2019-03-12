package org.openl.rules.datatype;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class WrongAliasDefaultValuesTest {

    @Test(expected = Exception.class)
    public void test1() {
        TestUtils.create("test/rules/datatype/WrongAliasDefaultValues1.xls");
    }

    @Test(expected = Exception.class)
    public void test2() {
        TestUtils.create("test/rules/datatype/WrongAliasDefaultValues2.xls");
    }

    @Test(expected = Exception.class)
    public void test3() {
        TestUtils.create("test/rules/datatype/WrongAliasDefaultValues3.xls");
    }

    @Test(expected = Exception.class)
    public void test4() {
        TestUtils.create("test/rules/datatype/WrongAliasDefaultValues4.xls");
    }

}
