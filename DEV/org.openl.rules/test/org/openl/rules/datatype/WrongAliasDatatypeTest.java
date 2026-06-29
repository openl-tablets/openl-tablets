package org.openl.rules.datatype;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class WrongAliasDatatypeTest {

    @Test
    void test1() {
        TestUtils.create("test/rules/datatype/WrongAliasDatatype1Test.xlsx");
    }

    @Test
    void test2() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatype2Test.xlsx");
        });
    }

    @Test
    void test3() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatype3Test.xlsx");
        });
    }

    @Test
    void test4() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatype4Test.xlsx");
        });
    }

    @Test
    void test6() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage2Test.xlsx");
        });
    }

    @Test
    void test7() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage3Test.xlsx");
        });
    }

    @Test
    void test8() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage4Test.xlsx");
        });
    }

    @Test
    void test9() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage5Test.xlsx");
        });
    }

    @Test
    void test10() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDatatypeUsage6Test.xlsx");
        });
    }
}
