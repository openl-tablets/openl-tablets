package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

class MultiCallMethodTest {

    private static final String SRC = "test/rules/binding/ArrayMethodsTest.xlsx";

    private static ArrayMethodsInterf instance;

    @BeforeAll
    static void init() {
        instance = TestUtils.create(SRC, ArrayMethodsInterf.class);
    }

    @Test
    void testSingleCall() {
        assertEquals(25, instance.start());
    }

    @Test
    void testArrayCall() {
        assertEquals(45, instance.start1()[1]);
    }

    @Test
    void testInt() {
        assertEquals(5, instance.intTest(4));
        assertEquals(7, instance.intArrayTest(new int[]{4, 5, 6})[2]);
    }

    @Test
    void testAccessFieldsMethodsForDataTable() {
        String[] names = instance.personsNames();
        assertEquals(2, names.length);
        assertEquals("Vasia", names[0]);
        assertEquals("Petia", names[1]);
    }

    @Test
    void testAccessFieldsMethodsForArrays() {
        String[] names = instance.personNamesFromArray();
        assertEquals(2, names.length);
        assertEquals("Vasia", names[0]);
        assertEquals("Petia", names[1]);
    }

    @Test
    void test2MethodsCall() {
        int[] a = instance.test2MethodCalls();
        assertEquals(2, a.length);
        assertEquals(7, a[0]);
        assertEquals(9, a[1]);
    }

    @Test
    void testVoidCallFromTBasic() {
        instance.TBasicCall();
    }

    public interface ArrayMethodsInterf {
        int start();

        int[] start1();

        int intTest(int a);

        int[] intArrayTest(int[] b);

        String[] personsNames();

        String[] personNamesFromArray();

        int[] test2MethodCalls();

        void TBasicCall();
    }

}
