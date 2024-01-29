package org.openl.rules.datatype;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

public class WrongAliasDefaultValuesTest {

    @Test
    public void test1() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDefaultValues1.xls");
        });
    }

    @Test
    public void test2() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDefaultValues2.xls");
        });
    }

    @Test
    public void test3() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDefaultValues3.xls");
        });
    }

    @Test
    public void test4() {
        assertThrows(Exception.class, () -> {
            TestUtils.create("test/rules/datatype/WrongAliasDefaultValues4.xls");
        });
    }

}
