package org.openl.ie.constrainer.impl;

import java.util.HashMap;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.constrainer.impl.IntExpAddValue;

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

public class TestIntExpAddValue extends TestCase {

    private Constrainer C = new Constrainer("TestIntExpAddValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpAddValue.class));
    }

    public TestIntExpAddValue(String name) {
        super(name);
    }

    public void testAdd() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp sumexp = new IntExpAddValue(intvar, 10);
        IntExp sumsumexp = sumexp.add(5);
        assertEquals(-10 + 10 + 5, sumsumexp.min());
        assertEquals(10 + 10 + 5, sumsumexp.max());
        try {
            intvar.setMin(-5);
            C.propagate();
            assertEquals(-5 + 10 + 5, sumsumexp.min());
            intvar.setMax(9);
            C.propagate();
            assertEquals(9 + 10 + 5, sumsumexp.max());
            sumexp.setMin(sumexp.min() + 2);
            C.propagate();
            assertEquals(sumexp.min() + 5, sumsumexp.min());
            sumexp.setMax(sumexp.max() - 2);
            C.propagate();
            assertEquals(sumexp.max() + 5, sumsumexp.max());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax() or IntVar.setMin()");
        }

        try {
            sumexp.setValue(sumexp.min());
            C.propagate();
            try {
                assertEquals(sumexp.value() + 5, sumsumexp.value());
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntExpAddValue.add(int)");
            }
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setValue()");
        }
    } // end of testAdd()

    public void testAttachDetachObserver() {
        IntVar intvar1 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp sum = new IntExpAddValue(intvar1, 5);
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
            sum.setMax(10);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int)");
        }

        assertEquals(2, observer1.updtCounter());
        // observer2.update() hasn't to be invoked!
        assertEquals(1, observer2.updtCounter());
    } // end of testAttachObserver()

    public void testBound() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp sumexp = new IntExpAddValue(intvar, 10);
        assertTrue(!sumexp.bound());
        try {
            intvar.setMin(10);
            assertTrue(sumexp.bound());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int)");
        }
    }

    public void testCalcCoeffs() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp sumexp = new IntExpAddValue(intvar, 10);
        assertTrue(sumexp.isLinear());
        try {
            assertEquals(3 * 10.0, sumexp.calcCoeffs(new HashMap(), 3), 0);
        } catch (NonLinearExpression ex) {
            fail("failed!!!");
        }
    }

    public void testContains() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp sumexp = new IntExpAddValue(intvar, 10);
        assertTrue(!sumexp.contains(intvar.min() + 10 - 1));
        assertTrue(!sumexp.contains(intvar.max() + 10 + 1));
        assertTrue(sumexp.contains(intvar.min() + 10));
        assertTrue(sumexp.contains(intvar.max() + 10));
        try {
            intvar.removeValue(0);
            C.propagate();
            assertTrue(!sumexp.contains(0 + 10));
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue(int)");
        }
    }

    public void testMaxMin() {
        IntVar intvar1 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp sumexp = new IntExpAddValue(intvar1, 5);
        assertEquals(10 + 5, sumexp.max());
        assertEquals(-10 + 5, sumexp.min());
        try {
            intvar1.setMax(9);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax()");
        }
        assertEquals(9 + 5, sumexp.max());
        try {
            intvar1.setMin(-9);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin()");
        }
        assertEquals(-9 + 5, sumexp.min());
    }

    public void testRemoveValue() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp sumexp = new IntExpAddValue(intvar, 5);
        int oldSumexpMax = sumexp.max();
        int oldSumexpMin = sumexp.min();
        int oldIntVarMax = intvar.max();
        int oldIntVarMin = intvar.min();
        try {
            sumexp.removeValue(sumexp.max());
            C.propagate();
            assertEquals(oldSumexpMax - 1, sumexp.max());
            assertEquals(oldIntVarMax - 1, intvar.max());
            sumexp.removeValue(sumexp.min());
            C.propagate();
            assertEquals(oldSumexpMin + 1, sumexp.min());
            assertEquals(oldIntVarMin + 1, intvar.min());

            sumexp.removeValue(0);
            C.propagate();
            assertTrue(!intvar.contains(0 - 5));
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue()");
        }
    }

    public void testSetMaxSetMin() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp sumexp = new IntExpAddValue(intvar, 5);
        try {
            sumexp.setMax(12);
            assertEquals(12 - 5, intvar.max());
            sumexp.setMin(0);
            assertEquals(0 - 5, intvar.min());
            intvar.setMax(5);
            try {
                sumexp.setMin(6 + 5);
                fail("test failed!");
            } catch (Failure f) {
            }
            intvar.setMin(3);
            try {
                sumexp.setMax(2 + 5);
                fail("test failed!");
            } catch (Failure f) {
            }
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin() or IntVar.setMax()");
        }
    }

    public void testSetValue() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp sumexp = new IntExpAddValue(intvar, 5);
        try {
            sumexp.setValue(5);
            assertEquals(5 - 5, intvar.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setValue(int)");
        }

        intvar = C.addIntVar(-10, 10, "intvar");
        sumexp = new IntExpAddValue(intvar, 5);
        try {
            intvar.removeValue(0);
            sumexp.setValue(0 + 5);
            fail("allow to assign missing values");
        } catch (Failure f) {
        }

    }
}