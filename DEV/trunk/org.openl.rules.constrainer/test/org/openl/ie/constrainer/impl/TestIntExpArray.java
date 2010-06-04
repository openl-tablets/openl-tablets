package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntArray;
import org.openl.ie.constrainer.IntArrayCards;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestIntExpArray extends TestCase {
    Constrainer C = new Constrainer("");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpArray.class));
    }

    public TestIntExpArray(String name) {
        super(name);
    }

    public void testCards() {
        IntExpArray array = new IntExpArray(C, 21, -10, 10, "array");
        try {
            IntArrayCards cards = array.cards();
            assertEquals(array.max() - array.min() + 1, cards.cardSize());
            for (int i = 0; i < cards.cardSize(); i++) {
                C.postConstraint(cards.cardAt(array.min() + i).eq(1));
            }
            boolean flag = C.execute(new GoalGenerate(array));
            assertEquals(true, flag);
            int[] values = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                values[i] = array.get(i).value();
                System.out.println(": " + values[i]);
            }
            assertTrue(TestUtils.isAllDiff(values));
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testMinMax() {
        IntExpArray array = new IntExpArray(C, 10);
        int[] min = { 1, 2, -21, 12, 14, -3, 0, 0, 4, -21 }, max = { 2, 3, -20, 15, 17, -2, 0, 17, 5, -21 };
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addIntVar(min[i], max[i]), i);
        }
        assertEquals(array.min(), -21);
        assertEquals(array.max(), 17);
    }

    public void testMulIntArray() {
        IntArray intarray = new IntArray(C, new int[] { 1, 0 });
        IntExpArray exparray = new IntExpArray(C, 2, 0, 1, "exparray");
        try {
            C.postConstraint(exparray.get(0).eq(intarray.elementAt(exparray.get(1))));
            C.postConstraint(exparray.get(1).eq(intarray.elementAt(exparray.get(0))));

            C.postConstraint(exparray.mul(intarray).eq(0));

            assertEquals(0, exparray.get(0).value());
            assertEquals(1, exparray.get(1).value());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testMulIntExpArray() {
        IntExpArray array1 = new IntExpArray(C, 10, 1, 10, "array1"), array2 = new IntExpArray(C, 10, 1, 10, "array2");

        try {
            IntExp product = array1.mul(array1);
            for (int i = 0; i < array1.size(); i++) {
                array1.get(i).setValue(i + 1);
            }
            C.propagate();
            assertEquals(product.value(), 385);

            product = array1.mul(array2);

            C.postConstraint(product.eq(550));
            C.execute(new GoalGenerate(array2));
            for (int i = 0; i < array2.size(); i++) {
                assertEquals(array2.get(i).value(), 10);
            }
        } catch (Failure f) {
            fail("Test failed!");
        }

        try {
            array1 = new IntExpArray(C, 2);
            array2 = new IntExpArray(C, 2);

            array1.set(C.addIntVar(-5, -1), 0);
            array1.set(C.addIntVar(1, 5), 1);
            array2.set(C.addIntVar(1, 5), 0);
            array2.set(C.addIntVar(1, 5), 1);

            // event propagation test
            C.postConstraint(array1.mul(array2).eq(0));
            array1.get(0).setValue(-1);
            array2.get(1).setValue(5);
            C.propagate();
            assertEquals(1, array1.get(1).value());
            assertEquals(5, array2.get(0).value());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSum() {
        IntExpArray array = new IntExpArray(C, 10, -5, 5, "array");
        try {
            C.postConstraint(array.sum().eq(45));
        } catch (Failure f) {
            fail("test failed");
        }

        try {
            array.get(0).setMax(-1);
            fail("test failed");
        } catch (Failure f) {
        }

        boolean flag = C.execute(new GoalGenerate(array));
        assertTrue(flag);
        int sum = 0;
        for (int i = 0; i < array.size(); i++) {
            try {
                sum += array.get(i).value();
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.value()");
            }
        }
        assertEquals(45, sum);

        array = new IntExpArray(C, 2, -5, 5, "array1");
        // event propagation test
        try {
            C.postConstraint(array.sum().eq(0));
            array.get(0).setValue(-5);
            C.propagate();
            assertEquals(5, array.get(1).value());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    // public void test

}