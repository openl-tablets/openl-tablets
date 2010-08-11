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
import org.openl.ie.constrainer.impl.IntExpMultiplyPositive;

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
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class TestIntExpMultiplyPositive extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpMultiplyPositive");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpMultiplyPositive.class));
    }

    public TestIntExpMultiplyPositive(String name) {
        super(name);
    }

    public void testAttachDetachObserver() {
        IntVar intvar1 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp mul = new IntExpMultiplyPositive(intvar1, 2);
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
        mul.attachObserver(observer1);
        mul.attachObserver(observer2);
        assertEquals(0, observer1.updtCounter());
        assertEquals(0, observer2.updtCounter());
        try {
            mul.setMin(-10);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int)");
        }
        assertEquals(1, observer1.updtCounter());
        assertEquals(1, observer1.updtCounter());

        mul.detachObserver(observer2);
        try {
            mul.setMax(10);
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
        IntExp mulexp = new IntExpMultiplyPositive(intvar, 10);
        assertTrue(!mulexp.bound());
        try {
            intvar.setMin(10);
            assertTrue(mulexp.bound());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int)");
        }
    }

    public void testCalcCoeffs() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp mulexp = new IntExpMultiplyPositive(intvar.add(3), 10);
        assertTrue(mulexp.isLinear());
        try {
            HashMap map = new HashMap();
            assertEquals(3 * 10.0, mulexp.calcCoeffs(map), 0);
            assertEquals(10.0, ((Double) map.get(intvar)).doubleValue(), 0);
        } catch (NonLinearExpression ex) {
            fail("failed!!!");
        }
    }

    public void testContains() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp mulexp = new IntExpMultiplyPositive(intvar, 10);
        assertTrue(!mulexp.contains(intvar.min() * 10 - 1));
        assertTrue(!mulexp.contains(intvar.max() * 10 + 1));
        assertTrue(mulexp.contains(intvar.min() * 10));
        assertTrue(mulexp.contains(intvar.max() * 10));
        assertTrue(!mulexp.contains(8));
        assertTrue(!mulexp.contains(-5));
        assertTrue(mulexp.contains(0));
        try {
            intvar.removeValue(0);
            C.propagate();
            assertTrue(!mulexp.contains(0));
            intvar.removeValue(-2);
            C.propagate();
            assertTrue(!mulexp.contains(-11));
            assertTrue(!mulexp.contains(-20));
            assertTrue(!mulexp.contains(-12));
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue(int)");
        }
    }

    public void testMaxMin() {
        IntVar intvar1 = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp mulexp = new IntExpMultiplyPositive(intvar1, 5);
        assertEquals(10 * (5), mulexp.max());
        assertEquals(-10 * (5), mulexp.min());
        try {
            intvar1.setMax(9);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax()");
        }
        assertEquals(9 * (5), mulexp.max());
        try {
            intvar1.setMin(-9);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin()");
        }
        assertEquals(-9 * (5), mulexp.min());
    }

    public void testMul() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp mulexp = new IntExpMultiplyPositive(intvar, 10);
        IntExp mulmulexp = mulexp.mul(5);
        assertEquals(-10 * 10 * 5, mulmulexp.min());
        assertEquals(10 * 10 * 5, mulmulexp.max());
        try {
            intvar.setMin(-5);
            C.propagate();
            assertEquals(-5 * 10 * 5, mulmulexp.min());
            intvar.setMax(9);
            C.propagate();
            assertEquals(9 * 10 * 5, mulmulexp.max());
            mulexp.setMin(mulexp.min() + 2);
            C.propagate();
            assertEquals(mulexp.min() * 5, mulmulexp.min());
            mulexp.setMax(mulexp.max() - 2);
            C.propagate();
            assertEquals(mulexp.max() * 5, mulmulexp.max());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax() or IntVar.setMin()");
        }

        try {
            mulexp.setValue(mulexp.min());
            C.propagate();
            try {
                assertEquals(mulexp.value() * 5, mulmulexp.value());
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntExpMultiplyPositive.add(int)");
            }
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setValue()");
        }
    } // end of testMul()

    public void testRemoveValue() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp mulexp = new IntExpMultiplyPositive(intvar, 5);
        int oldmulexpMax = mulexp.max();
        int oldmulexpMin = mulexp.min();
        int oldIntVarMax = intvar.max();
        int oldIntVarMin = intvar.min();
        try {
            mulexp.removeValue(mulexp.max());
            C.propagate();
            assertEquals(oldmulexpMax - 5, mulexp.max());
            assertEquals(oldIntVarMax - 1, intvar.max());
            mulexp.removeValue(mulexp.min());
            C.propagate();
            assertEquals(oldmulexpMin + 5, mulexp.min());
            assertEquals(oldIntVarMin + 1, intvar.min());

            mulexp.removeValue(5);
            C.propagate();
            assertTrue(!intvar.contains(1));
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue()");
        }
    }

    public void testSetMaxSetMin() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar");
        IntExp mulexp = new IntExpMultiplyPositive(intvar, 5);
        try {
            mulexp.setMax(42);
            assertEquals((42 - 42 % 5), mulexp.max());
            assertEquals(42 / 5, intvar.max());

            mulexp.setMin(-33);
            assertEquals((-33 - (-33) % 5), mulexp.min());
            assertEquals((-33 / 5), intvar.min());

            intvar.setMax(5);
            try {
                mulexp.setMin(5 * 5 + 1);
                fail("test failed!");
            } catch (Failure f) {
            }
            intvar.setMin(3);
            try {
                mulexp.setMax(3 * 5 - 1);
                fail("test failed!");
            } catch (Failure f) {
            }
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin() or IntVar.setMax()");
        }
    }

}