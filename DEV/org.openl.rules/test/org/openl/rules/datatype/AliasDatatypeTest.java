package org.openl.rules.datatype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.TestUtils;
import org.openl.rules.helpers.IntRange;

public class AliasDatatypeTest {

    private static final String SRC = "test/rules/datatype/AliasDatatypeTest.xlsx";

    public interface ITest {
        int test1(String state);

        int test2(int age);

        int test3(IntRange age);

        int method1();

        int method2();

        boolean method3(int z);

        String method4(String x);

        String method5(String x);

        int testStringAliasType(String state);

        int testAliasTypeAsArrays(String state);

        int testIntAliasType(int i);

        int testIntRangeAliasType2(int i);
    }

    @Test
    public void test1() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        int res = instance.test1("CA");
        assertEquals(1, res);
    }

    @Test
    public void test11() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        int res = instance.testStringAliasType("CA");
        assertEquals(1, res);
    }

    @Test(expected = RuntimeException.class)
    public void test2() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        instance.test1("Something that doesn't belong to domain");
    }

    @Test
    public void test3() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        int res = instance.test2(1);
        assertEquals(3, res);
    }

    @Test
    public void test31() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        int res = instance.testIntAliasType(1);
        assertEquals(3, res);
    }

    @Test(expected = Exception.class)
    public void test4() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        instance.test3(new IntRange(1, 3));
    }

    @Test
    public void test41() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        int res = instance.testIntRangeAliasType2(15);
        assertEquals(1, res);

        res = instance.testIntRangeAliasType2(1000);
        assertEquals(0, res);
    }

    @Test(expected = RuntimeException.class)
    public void test5() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        instance.method1();
    }

    @Test
    public void test6() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        int res = instance.method2();
        assertEquals(1, res);
    }

    @Test
    public void test7() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        boolean res = instance.method3(5);
        assertTrue(res);

        res = instance.method3(-1);
        assertFalse(res);
    }

    @Test
    public void test8() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        String res = instance.method4("New York");
        assertEquals("New York", res);

        String res2 = instance.method5("New York");
        assertEquals("New York", res2);
    }

    @Test
    public void testArrays() {

        ITest instance = TestUtils.create(SRC, ITest.class);
        int res = instance.testAliasTypeAsArrays("AR");
        assertEquals(1, res);

        res = instance.testAliasTypeAsArrays("NY");
        assertEquals(2, res);
    }
}
