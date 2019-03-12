package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class MultiCallMethodTest {

    private static final String SRC = "test/rules/binding/ArrayMethodsTest.xlsx";

    private static ArrayMethodsInterf instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC, ArrayMethodsInterf.class);
    }

    @Test
    public void testSingleCall() {
        assertEquals(25, instance.start());
    }

    @Test
    public void testArrayCall() {
        assertEquals(45, instance.start1()[1]);
    }

    @Test
    public void testInt() {
        assertEquals(5, instance.intTest(4));
        assertEquals(7, instance.intArrayTest(new int[] { 4, 5, 6 })[2]);
    }

    @Test
    public void testAccessFieldsMethodsForDataTable() {
        String[] names = instance.personsNames();
        assertTrue(names.length == 2);
        assertEquals("Vasia", names[0]);
        assertEquals("Petia", names[1]);
    }

    @Test
    public void testAccessFieldsMethodsForArrays() {
        String[] names = instance.personNamesFromArray();
        assertTrue(names.length == 2);
        assertEquals("Vasia", names[0]);
        assertEquals("Petia", names[1]);
    }

    @Test
    public void test2MethodsCall() {
        int[] a = instance.test2MethodCalls();
        assertTrue(a.length == 2);
        assertEquals(7, a[0]);
        assertEquals(9, a[1]);
    }

    @Test
    public void testVoidCallFromTBasic() {
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
