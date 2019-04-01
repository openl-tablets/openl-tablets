package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestIntExpAddArray1 extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpAddArray1");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpAddArray1.class));
    }

    public TestIntExpAddArray1(String name) {
        super(name);
    }

    public void testAttachDetachObserver() {
        IntExpAddArray1 sum = new IntExpAddArray1(C, new IntExpArray(C, C.addIntVar(-5, 5)));
        class TestObserver extends Observer {
            private int counter = 0;

            @Override
            public Object master() {
                return null;
            }

            @Override
            public int subscriberMask() {
                return MIN | MAX | VALUE;
            }

            @Override
            public void update(Subject exp, EventOfInterest event) throws Failure {
                counter++;
            }

            public int updtCounter() {
                return counter;
            }
        } // end of TestObserver
        TestObserver observer1 = new TestObserver(), observer2 = new TestObserver();
        sum.attachObserver(observer1);
        sum.attachObserver(observer2);
        assertEquals(0, observer1.updtCounter());
        assertEquals(0, observer2.updtCounter());
        try {
            sum.setMax(4);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int)");
        }
        assertEquals(1, observer1.updtCounter());
        assertEquals(1, observer1.updtCounter());

        sum.detachObserver(observer2);
        try {
            sum.setMax(1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int)");
        }

        assertEquals(2, observer1.updtCounter());
        // observer2.update() hasn't to be invoked!
        assertEquals(1, observer2.updtCounter());

    } // end of testAttachDetachObserver()

    public void testMaxMin() {
        IntExpArray array = new IntExpArray(C, 10, -10, 10, "array");
        IntExpAddArray1 sum = new IntExpAddArray1(C, array);
        assertEquals(10 * 10, sum.max());
        assertEquals(-10 * 10, sum.min());
        for (int i = 0; i < array.size(); i++) {
            try {
                IntExp expi = array.get(i);
                expi.setMax(10 - i);
                expi.setMin(-10 + i);
                C.propagate();
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMax() or IntVar.setMin()");
            }
        }
        assertEquals(55, sum.max());
        assertEquals(-55, sum.min());
    }

    public void testRemoveValue() {
        IntExpArray array = new IntExpArray(C, 10, 0, 5, "array");
        IntExpAddArray1 sum = new IntExpAddArray1(C, array);
        try {
            sum.removeValue(0);
            C.propagate();
            assertEquals(1, sum.min());
            sum.removeValue(50);
            assertEquals(49, sum.max());
        } catch (Failure f) {
            fail("");
        }
    }

    public void testSetMax() {
        IntExpArray array = new IntExpArray(C, 10, 1, 10, "array");
        IntExpAddArray1 sum = new IntExpAddArray1(C, array);
        // setting sum[i=1..10](array[i].min()) as maxValue has to result in
        // assigning values to
        // all entries of the array
        try {
            sum.setMax(10);
            C.propagate();
            assertEquals(10, sum.max());
            for (int i = 0; i < array.size(); i++) {
                assertEquals(1, array.get(i).max());
            }
        } catch (Failure f) {
            fail("test failed due to incorrect work of setMax(int)");
        }

        // setting (sum[i=1..10](array[i].min())-1) as maxValue has to result in
        // throwing Failure
        array = new IntExpArray(C, 10, 1, 10, "array");
        sum = new IntExpAddArray1(C, array);
        try {
            sum.setMax(9);
            fail("allow to assign maxvalue that less then sum[i=1..n](array[i].min())");
        } catch (Failure f) {
        }

        array = new IntExpArray(C, 10, 1, 10, "array");
        sum = new IntExpAddArray1(C, array);
        try {
            array.get(1).removeValue(1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntExp.removeValue(int)");
        }
        try {
            sum.setMax(10);
            fail("allow to assign maxvalue that less then sum[i=1..n](array[i].min())");
        } catch (Failure f) {
        }

        try {
            try {
                sum.setMax(15);
                C.propagate();
            } catch (Failure f) {
                fail("incorrect work of setMax(int)");
            }
            array.get(1).setMin(7);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testSetMin() {
        IntExpArray array = new IntExpArray(C, 10, -10, -1, "array");
        IntExpAddArray1 sum = new IntExpAddArray1(C, array);
        try {
            sum.setMin(-10);
            C.propagate();
            assertEquals(-10, sum.min());
            for (int i = 0; i < array.size(); i++) {
                assertEquals(-1, array.get(i).min());
            }
        } catch (Failure f) {
            fail("test failed due to incorrect work of setMax(int)");
        }

        array = new IntExpArray(C, 10, -10, -1, "array");
        sum = new IntExpAddArray1(C, array);
        try {
            sum.setMin(-9);
            fail("allow to assign minvalue that greater then sum[i=1..n](array[i].max())");
        } catch (Failure f) {
        }

        array = new IntExpArray(C, 10, -10, -1, "array");
        sum = new IntExpAddArray1(C, array);
        try {
            array.get(1).removeValue(-1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntExp.removeValue(int)");
        }
        try {
            sum.setMin(-10);
            fail("allow to assign minvalue that greater then sum[i=1..n](array[i].max())");
        } catch (Failure f) {
        }

        try {
            try {
                sum.setMin(-15);
                C.propagate();
            } catch (Failure f) {
                fail("incorrect work of setMin(int)");
            }
            array.get(1).setMax(-7);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testSetValue() {
        IntExpArray array = new IntExpArray(C, 10, 0, 5, "array");
        IntExpAddArray1 sum = new IntExpAddArray1(C, array);
        try {
            sum.setValue(51);
            fail("allow to assign a value that greater then sum[i=1..10](array[i].max())");
        } catch (Failure f) {
        }

        try {
            sum.setValue(50);
            for (int i = 0; i < array.size(); i++) {
                assertEquals(5, array.get(i).value());
            }
        } catch (Failure f) {
        }
    }

}