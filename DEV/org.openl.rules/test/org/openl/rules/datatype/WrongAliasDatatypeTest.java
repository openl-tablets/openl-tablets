package org.openl.rules.datatype;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

public class WrongAliasDatatypeTest {

    @Test
    public void test1() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatype1Test.xlsx");
    }

    @Test
    public void test2() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatype2Test.xlsx");
        });
    }

    @Test
    public void test3() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatype3Test.xlsx");
        });
    }

    @Test
    public void test4() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatype4Test.xlsx");
        });
    }

    @Test
    public void test6() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage2Test.xlsx");
        });
    }

    @Test
    public void test7() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage3Test.xlsx");
        });
    }

    @Test
    public void test8() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage4Test.xlsx");
        });
    }

    @Test
    public void test9() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage5Test.xlsx");
        });
    }

    @Test
    public void test10() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage6Test.xlsx");
        });
    }
}
