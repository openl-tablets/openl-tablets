package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class MultiArgumentArrayMethodTest {
    private static final String SRC = "test/rules/binding/MultiArgumentArrayMethodTest.xls";

    private static MultiArgumentArrayMethodInterf instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create(SRC, MultiArgumentArrayMethodInterf.class);
    }

    @Test
    public void testMultiArgumentsCall() {
        assertEquals(16, instance.callMultiArguments());
    }

    @Test
    public void testMultiArgumentsArrayCall() {
        // test calling multi arguments method with 1 array argument
        assertEquals(2, instance.callMultiArgumentsArray().length);
        assertEquals(16, instance.callMultiArgumentsArray()[0]);
        assertEquals(17, instance.callMultiArgumentsArray()[1]);
    }

    @Test
    public void testMultiArgumentsArrayCall1() {
        // test calling multi arguments method with 2 array argument
        assertEquals(2, instance.callMultiArgumentsArray1().length);
        assertEquals(26, instance.callMultiArgumentsArray1()[0]);
        assertEquals(27, instance.callMultiArgumentsArray1()[1]);
    }

    @Test
    public void testMultiArgumentsArrayCall2() {
        // test calling multi arguments method with 3 array argument
        assertEquals(3, instance.callMultiArgumentsArray2().length);
        assertEquals(25, instance.callMultiArgumentsArray2()[0]);
        assertEquals(26, instance.callMultiArgumentsArray2()[1]);
        assertEquals(27, instance.callMultiArgumentsArray2()[2]);
    }

    @Test
    public void testMultiArgumentsArrayCall3() {
        assertEquals(6, instance.callMultiArgumentsArray3().length);
        assertEquals(24, instance.callMultiArgumentsArray3()[0]);
        assertEquals(25, instance.callMultiArgumentsArray3()[1]);
        assertEquals(26, instance.callMultiArgumentsArray3()[2]);
        assertEquals(25, instance.callMultiArgumentsArray3()[3]);
        assertEquals(26, instance.callMultiArgumentsArray3()[4]);
        assertEquals(27, instance.callMultiArgumentsArray3()[5]);
    }

    @Test
    public void testMultiArgumentsArrayCall4() {
        assertEquals(12, instance.callMultiArgumentsArray4().length);

        assertEquals(13, instance.callMultiArgumentsArray4()[0]);
        assertEquals(14, instance.callMultiArgumentsArray4()[1]);
        assertEquals(15, instance.callMultiArgumentsArray4()[2]);

        assertEquals(14, instance.callMultiArgumentsArray4()[3]);
        assertEquals(15, instance.callMultiArgumentsArray4()[4]);
        assertEquals(16, instance.callMultiArgumentsArray4()[5]);

        assertEquals(22, instance.callMultiArgumentsArray4()[6]);
        assertEquals(23, instance.callMultiArgumentsArray4()[7]);
        assertEquals(24, instance.callMultiArgumentsArray4()[8]);

        assertEquals(23, instance.callMultiArgumentsArray4()[9]);
        assertEquals(24, instance.callMultiArgumentsArray4()[10]);
        assertEquals(25, instance.callMultiArgumentsArray4()[11]);
    }

    @Test
    public void testMultiArgumentsArrayCall5() {
        assertEquals(6, instance.callMultiArgumentsArray5().length);

        assertEquals(14, instance.callMultiArgumentsArray5()[0]);
        assertEquals(15, instance.callMultiArgumentsArray5()[1]);
        assertEquals(16, instance.callMultiArgumentsArray5()[2]);

        assertEquals(23, instance.callMultiArgumentsArray5()[3]);
        assertEquals(24, instance.callMultiArgumentsArray5()[4]);
        assertEquals(25, instance.callMultiArgumentsArray5()[5]);
    }

    @Test
    public void testMultiArgumentsArrayWithNullArgument() {
        assertEquals(0, instance.callMultiArgumentsArrayWithNullArgument().length);
    }

    @Test
    public void testArrayCall1() {
        // check calling array of arrays
        assertEquals(2, instance.testArrayCall1().length);
        assertEquals(30, instance.testArrayCall1()[0]);
        assertEquals(31, instance.testArrayCall1()[1]);
    }

    @Test
    public void testArrayCall2() {
        // check calling array of arrays
        assertEquals(2, instance.testArrayCall2().length);
        assertEquals(30, instance.testArrayCall1()[0]);
        assertEquals(31, instance.testArrayCall1()[1]);
    }

    @Test
    public void testVoidMultiCall() {
        // calling method with void return type many times
        //
        instance.callVoidMethod();
    }

    public interface MultiArgumentArrayMethodInterf {
        int callMultiArguments();

        int[] callMultiArgumentsArray();

        int[] callMultiArgumentsArray1();

        int[] callMultiArgumentsArray2();

        int[] callMultiArgumentsArray3();

        int[] callMultiArgumentsArray4();

        int[] callMultiArgumentsArray5();

        int[] callMultiArgumentsArrayWithNullArgument();

        int[] testArrayCall1();

        int[] testArrayCall2();

        void callVoidMethod();
    }
}
