package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.constrainer.impl.IntExpAddExp;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigen Group, Inc.
 * </p>
 *
 * @author Sergej Vanskov
 * @version 1.0
 */

public class TestIntExpAddExp extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpAbs");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpAddExp.class));
    }

    public TestIntExpAddExp(String name) {
        super(name);
    }

    public void testAttachDetachObserver() {
        IntVar intvar1 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST),
                intvar2 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp sum = new IntExpAddExp(intvar1, intvar2);
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
            // sum.setMin(5);
            sum.setMin(5);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int)");
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
    } // end of testAttachObserver()

    public void testCalc_MaxAndCalc_Min() {
        IntVar intvar1 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST),
                intvar2 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExpAddExp sum = new IntExpAddExp(intvar1, intvar2);

        assertEquals(20, sum.calc_max());
        assertEquals(-20, sum.calc_min());

        try {
            intvar1.removeValue(10);
            intvar2.removeValue(-10);
        } catch (Failure f) {
            fail("test failed");
        }

        assertEquals(19, sum.calc_max());
        assertEquals(-19, sum.calc_min());
    }

    public void testMaxMin() {
        IntVar intvar1 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST),
                intvar2 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp sum = new IntExpAddExp(intvar1, intvar2);
        assertEquals(10 * 2, sum.max());
        assertEquals(-10 * 2, sum.min());
        try {
            intvar1.setMax(9);
            intvar2.setMax(-5);
            intvar2.setMin(-9);
            intvar1.setMin(5);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax() or IntVar.setMin()");
        }
        assertEquals(4, sum.max());
        assertEquals(-4, sum.min());
    }

    public void testSetMax() {
        IntVar intvar1 = C.addIntVar(1, 10, "intvar", IntVar.DOMAIN_BIT_FAST),
                intvar2 = C.addIntVar(1, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExpAddExp sum = new IntExpAddExp(intvar1, intvar2);
        // setting sum[i=1..10](array[i].min()) as maxValue has to result in
        // assigning values to
        // all entries of the array
        try {
            sum.setMax(2);
            C.propagate();
            assertEquals(2, sum.max());
            assertEquals(1, intvar1.max());
            assertEquals(1, intvar2.max());
        } catch (Failure f) {
            fail("test failed due to incorrect work of setMax(int)");
        }

        // setting (sum[i=1..10](array[i].min())-1) as maxValue has to result in
        // throwing Failure
        intvar1 = C.addIntVar(1, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        intvar2 = C.addIntVar(1, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        sum = new IntExpAddExp(intvar1, intvar2);
        try {
            sum.setMax(1);
            fail("allow to assign maxvalue that less then sum[i=1..n](array[i].min())");
        } catch (Failure f) {
        }

        intvar1 = C.addIntVar(1, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        intvar2 = C.addIntVar(1, 10, "intvar", IntVar.DOMAIN_BIT_FAST);

        sum = new IntExpAddExp(intvar1, intvar2);
        try {
            intvar1.removeValue(1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntExp.removeValue(int)");
        }
        try {
            sum.setMax(2);
            fail("allow to assign maxvalue that less then sum[i=1..n](array[i].min())");
        } catch (Failure f) {
        }

        try {
            try {
                sum.setMax(8);
                C.propagate();
            } catch (Failure f) {
                fail("incorrect work of setMax(int)");
            }
            intvar2.setMin(7);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testSetMin() {
        IntVar intvar1 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST),
                intvar2 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExpAddExp sum = new IntExpAddExp(intvar1, intvar2);
        // setting sum[i=1..10](array[i].min()) as maxValue has to result in
        // assigning values to
        // all entries of the array
        try {
            sum.setMin(-2);
            C.propagate();
            assertEquals(-2, sum.min());
            assertEquals(-1, intvar1.min());
            assertEquals(-1, intvar2.min());
        } catch (Failure f) {
            fail("test failed due to incorrect work of setMax(int)");
        }

        // setting (sum[i=1..10](array[i].min())-1) as maxValue has to result in
        // throwing Failure
        intvar1 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST);
        intvar2 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST);
        sum = new IntExpAddExp(intvar1, intvar2);
        try {
            sum.setMin(-1);
            fail("allow to assign maxvalue that less then sum[i=1..n](array[i].min())");
        } catch (Failure f) {
        }

        intvar1 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST);
        intvar2 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST);

        sum = new IntExpAddExp(intvar1, intvar2);
        try {
            intvar1.removeValue(-1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntExp.removeValue(int)");
        }
        try {
            sum.setMin(-2);
            fail("allow to assign maxvalue that less then sum[i=1..n](array[i].min())");
        } catch (Failure f) {
        }

        try {
            try {
                sum.setMin(-8);
                C.propagate();
            } catch (Failure f) {
                fail("incorrect work of setMax(int)");
            }
            intvar2.setMax(-7);
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testSetValue() {
        IntVar intvar1 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST),
                intvar2 = C.addIntVar(-10, -1, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExpAddExp sum = new IntExpAddExp(intvar1, intvar2);
        try {
            sum.setValue(-1);
            fail("allow to assign a value that greater then sum[i=1..10](array[i].max())");
        } catch (Failure f) {
        }

        try {
            sum.setValue(-20);
            assertEquals(-10, intvar1.value());
            assertEquals(-10, intvar2.value());
        } catch (Failure f) {
        }
    }

}