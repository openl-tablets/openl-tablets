package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.constrainer.impl.IntExpAbs;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestIntExpAbs extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpAbs");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpAbs.class));
    }

    public TestIntExpAbs(String name) {
        super(name);
    }

    public void testAttachDetachObserver() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp varabs = new IntExpAbs(intvar);
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
        varabs.attachObserver(observer1);
        varabs.attachObserver(observer2);
        assertEquals(0, observer1.updtCounter());
        assertEquals(0, observer2.updtCounter());
        try {
            varabs.setMax(5);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int)");
        }
        assertEquals(1, observer1.updtCounter());
        assertEquals(1, observer1.updtCounter());

        varabs.detachObserver(observer2);
        try {
            varabs.setMax(1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int)");
        }

        assertEquals(2, observer1.updtCounter());
        // observer2.update() hasn't to be invoked!
        assertEquals(1, observer2.updtCounter());

    } // end of testAttachObserver()

    public void testContains() {
        IntVar intvar = C.addIntVar(-15, 10);
        IntExp varabs = new IntExpAbs(intvar);
        assertTrue(varabs.contains(15));
        assertTrue(varabs.contains(14));
        assertTrue(varabs.contains(12));
        assertTrue(varabs.contains(11));
        assertTrue(varabs.contains(10));
        assertTrue(varabs.contains(0));
        assertTrue(varabs.contains(5));
    }

    public void testMinMax() {
        IntVar intvar1 = C.addIntVar(1, 10), intvar2 = C.addIntVar(-5, 3), intvar3 = C.addIntVar(-5, -2);

        IntExp absexp1 = new IntExpAbs(intvar1), absexp2 = new IntExpAbs(intvar2), absexp3 = new IntExpAbs(intvar3);

        assertEquals(1, absexp1.min());
        assertEquals(10, absexp1.max());
        assertEquals(0, absexp2.min());
        assertEquals(5, absexp2.max());
        assertEquals(2, absexp3.min());
        assertEquals(5, absexp3.max());
    }

    public void testRemoveValue() {
        IntVar intvar1 = C.addIntVar(1, 10, "intvar1", IntVar.DOMAIN_BIT_FAST), intvar2 = C.addIntVar(-5, 3, "intvar2",
                IntVar.DOMAIN_BIT_FAST), intvar3 = C.addIntVar(-5, -2, "intvar3", IntVar.DOMAIN_BIT_FAST);

        IntExp absexp1 = new IntExpAbs(intvar1), absexp2 = new IntExpAbs(intvar2), absexp3 = new IntExpAbs(intvar3);

        try {
            absexp1.removeValue(10);
            absexp1.removeValue(1);
            C.propagate();
            assertEquals(2, intvar1.min());
            assertEquals(9, intvar1.max());

            absexp2.removeValue(3);
            absexp2.removeValue(5);
            C.propagate();
            assertEquals(-4, intvar2.min());
            assertEquals(2, intvar2.max());

            absexp3.removeValue(2);
            absexp3.removeValue(3);
            C.propagate();
            assertEquals(-5, intvar3.min());
            assertEquals(-4, intvar3.max());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testSetMaxSetMin() {
        IntVar intvar1 = C.addIntVar(1, 10), intvar2 = C.addIntVar(-5, 3), intvar3 = C.addIntVar(-5, -2);

        IntExp absexp1 = new IntExpAbs(intvar1), absexp2 = new IntExpAbs(intvar2), absexp3 = new IntExpAbs(intvar3);

        try {
            absexp1.setMax(3);
            absexp1.setMin(2);
            C.propagate();
            assertEquals(2, intvar1.min());
            assertEquals(3, intvar1.max());

            absexp2.setMax(4);
            absexp2.setMin(2);
            C.propagate();
            assertEquals(-4, intvar2.min());
            // assertTrue(!intvar2.contains(-1));
            // assertTrue(!intvar2.contains(0));
            // assertTrue(!intvar2.contains(1));
            assertEquals(3, intvar2.max());

            absexp3.setMax(4);
            absexp3.setMin(3);
            C.propagate();
            assertEquals(-4, intvar3.min());
            assertEquals(-3, intvar3.max());

        } catch (Failure f) {
        }
    }

    public void testSetValue() {
        IntVar intvar1 = C.addIntVar(1, 10), intvar2 = C.addIntVar(-5, 3), intvar3 = C.addIntVar(-5, -2), intvar4 = C
                .addIntVar(-5, 5), intvar5 = C.addIntVar(-5, 5, "intvar5", IntVar.DOMAIN_BIT_FAST);

        IntExp absexp1 = new IntExpAbs(intvar1), absexp2 = new IntExpAbs(intvar2), absexp3 = new IntExpAbs(intvar3), absexp4 = new IntExpAbs(
                intvar4), absexp5 = new IntExpAbs(intvar5);

        try {

            absexp1.setValue(3);
            C.propagate();
            assertEquals(3, intvar1.value());

            absexp2.setValue(4);
            C.propagate();
            assertEquals(-4, intvar2.max());
            assertEquals(-4, intvar2.min());

            absexp3.setValue(3);
            C.propagate();
            assertEquals(-3, intvar3.value());

            absexp4.setValue(4);
            C.propagate();
            assertEquals(4, intvar4.max());
            assertEquals(-4, intvar4.min());

            intvar5.removeValue(4);
            C.propagate();
            absexp5.setValue(4);
            assertEquals(-4, intvar5.value());

            try {
                IntVar intvar6 = C.addIntVar(-5, 5);
                IntExp absexp6 = new IntExpAbs(intvar6);
                intvar6.removeValue(4);
                intvar6.removeValue(-4);
                C.propagate();
                absexp6.setValue(4);
                fail("allow to assign a wrong(missing in the domain) value to a variable");
            } catch (Failure f) {
            }
        } catch (Failure f) {
            fail("test failed!");
        }
    }
}