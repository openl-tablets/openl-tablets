package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Undo;

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

public class TestIntVarImpl extends TestCase {
    private Constrainer C = new Constrainer("TestIntVarImpl");
    private int size = 21;
    private int min = -10;
    private int max = 10;
    private IntVar bit_fast = new IntVarImpl(C, min, max, "intvar1", IntVar.DOMAIN_BIT_FAST);
    private IntVar bit_small = new IntVarImpl(C, min, max, "intvar2", IntVar.DOMAIN_BIT_SMALL);
    private IntVar plain = new IntVarImpl(C, min, max, "intvar3", IntVar.DOMAIN_PLAIN);

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntVarImpl.class));
    }

    public TestIntVarImpl(String name) {
        super(name);
    }

    public void testA() {
        IntVar intvar = new IntVarImpl(C, 0, 10, "intvar", IntVar.DOMAIN_BIT_FAST);

    }

    public void testForceMax() {
        IntVar intvar = new IntVarImpl(C, -10, 10, "var1", IntVar.DOMAIN_BIT_FAST),
                intvar1 = new IntVarImpl(C, -10, 10, "var1", IntVar.DOMAIN_BIT_SMALL),
                intvar2 = new IntVarImpl(C, -10, 10, "var1", IntVar.DOMAIN_PLAIN);

        try {
            intvar.setMax(9);
            intvar1.setMax(9);
            intvar2.setMax(9);
        } catch (Failure f) {
            fail("test failed");
        }

        assertEquals(9, intvar.max());
        assertEquals(9, intvar1.max());
        assertEquals(9, intvar2.max());

        intvar.forceMax(11);
        intvar1.forceMax(11);
        intvar2.forceMax(11);

        assertEquals(11, intvar.max());
        assertEquals(11, intvar1.max());
        assertEquals(11, intvar2.max());
    }

    public void testPropagate() {
        TestUtils.TestObserver observer1 = TestUtils.createTestObserver(), observer2 = TestUtils.createTestObserver();
        IntVar intvar = new IntVarImpl(C, 0, 10);
        intvar.attachObserver(observer1);
        intvar.attachObserver(observer2);
        try {
            intvar.setMin(1);
            intvar.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals(1, observer1.updtCounter());
        assertEquals(1, observer2.updtCounter());

        intvar.detachObserver(observer2);
        try {
            intvar.setMax(9);
            intvar.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals(2, observer1.updtCounter());
        assertEquals(1, observer2.updtCounter());
    }

    public void testRemoveValue() {
        Undo[] bit_fastUndo = new Undo[bit_fast.size() / 2 + 1], bit_smallUndo = new Undo[bit_fast.size() / 2 + 1],
                plainUndo = new Undo[bit_fast.size() / 2 + 1];
        int counter = 0;
        for (int i = -10; i <= -1; i++) {
            bit_fastUndo[counter] = bit_fast.createUndo();
            bit_fastUndo[counter].undoable(bit_fast);
            try {
                bit_fast.removeValue(i);
                bit_fast.removeValue(-i);
            } catch (Failure f) {
                fail(
                    "test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
            }

            bit_smallUndo[i - min] = bit_small.createUndo();
            bit_smallUndo[i - min].undoable(bit_small);
            try {
                bit_small.removeValue(i);
                bit_small.removeValue(-i);
            } catch (Failure f) {
                fail(
                    "test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
            }

            plainUndo[i - min] = plain.createUndo();
            plainUndo[i - min].undoable(plain);
            try {
                plain.removeValue(i);
                plain.removeValue(-i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_PLAIN)");
            }

            int newMin = i + 1, newMax = -i - 1, curSize = newMax - newMin + 1;

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)", newMin, bit_fast.min());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)", newMin, bit_small.min());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)", newMin, plain.min());
            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)", newMax, bit_fast.max());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)", newMax, bit_small.max());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)", newMax, plain.max());

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong size", curSize, bit_fast.size());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong size", curSize, bit_small.size());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong size", curSize, plain.size());
            for (int j = newMin; j <= newMax; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", plain.contains(j));
            }

            for (int j = min; j < newMin; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", !bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", !bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", !plain.contains(j));
            }

            for (int j = newMax + 1; j <= max; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", !bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", !bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", !plain.contains(j));
            }
            counter++;
        }

        try {
            bit_fast.removeValue(0);
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        } catch (Failure f) {
        }

        try {
            bit_small.removeValue(0);
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        } catch (Failure f) {
        }

        try {
            plain.removeValue(0);
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_PLAIN)");
        } catch (Failure f) {
        }

        assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of bound()", bit_fast.bound());
        assertTrue("IntVarImpl.Domain_BIT_SMALL: wrong work of bound()", bit_small.bound());
        assertTrue("IntVarImpl.Domain_PLAIN: wrong work of contains(int)", plain.bound());
        try {
            assertEquals("IntVar.value() failed", 0, bit_fast.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        }
        try {
            assertEquals("IntVar.value() failed", 0, bit_small.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        }
        try {
            assertEquals("IntVar.value() failed", 0, bit_fast.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_PLAIN)");
        }
        /** **************************************************************************************************** */
        // restoration from undos effected in postorder
        counter = 0;
        for (int i = -1; i > -10; i--) {
            bit_fastUndo[counter].undo();
            bit_smallUndo[counter].undo();
            plainUndo[counter].undo();
            int curMin = min + counter;
            int curMax = max - counter;
            int curSize = curMax - curMin + 1;

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)", curMin, bit_fast.min());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)", curMin, bit_small.min());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)", curMin, plain.min());

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)", curMax, bit_fast.max());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)", curMax, bit_small.max());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)", curMax, plain.max());

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong size", curSize, bit_fast.size());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong size", curSize, bit_small.size());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong size", curSize, plain.size());
            for (int j = curMin; j <= curMax; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", plain.contains(j));
            }

            for (int j = min; j < curMin; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", !bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", !bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", !plain.contains(j));
            }
            counter++;
        }
    }

    public void testSetMaxAndCreateUndo() {
        Undo[] bit_fastUndo = new Undo[bit_fast.size()], bit_smallUndo = new Undo[bit_fast.size()],
                plainUndo = new Undo[bit_fast.size()];

        for (int i = 10; i >= -10; i--) {
            bit_fastUndo[max - i] = bit_fast.createUndo();
            bit_fastUndo[max - i].undoable(bit_fast);
            try {
                bit_fast.setMax(i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
            }

            bit_smallUndo[max - i] = bit_small.createUndo();
            bit_smallUndo[max - i].undoable(bit_small);
            try {
                bit_small.setMax(i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
            }

            plainUndo[max - i] = plain.createUndo();
            plainUndo[max - i].undoable(plain);
            try {
                plain.setMax(i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_PLAIN)");
            }

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of setMax(int)", i, bit_fast.max());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of setMax(int)", i, bit_small.max());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of setMax(int)", i, plain.max());
            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong size", 21 - (max - i), bit_fast.size());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong size", 21 - (max - i), bit_small.size());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong size", 21 - (max - i), plain.size());
            for (int j = max; j > i; j--) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", !bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", !bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", !plain.contains(j));
            }

            for (int j = i; j >= min; j--) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", plain.contains(j));
            }
        }

        assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of bound()", bit_fast.bound());
        assertTrue("IntVarImpl.Domain_BIT_SMALL: wrong work of bound()", bit_small.bound());
        assertTrue("IntVarImpl.Domain_PLAIN: wrong work of contains(int)", plain.bound());
        try {
            assertEquals("IntVar.value() failed", min, bit_fast.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        }
        try {
            assertEquals("IntVar.value() failed", min, bit_small.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        }
        try {
            assertEquals("IntVar.value() failed", min, bit_fast.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int) (Domain type: IntVar.DOMAIN_BIT_PLAIN)");
        }
        /** **************************************************************************************************** */
        // restoration from undos
        for (int i = -10; i < 10; i++) {
            bit_fastUndo[max - i].undo();
            bit_smallUndo[max - i].undo();
            plainUndo[max - i].undo();

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of setMax(int)", i + 1, bit_fast.max());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of setMax(int)", i + 1, bit_small.max());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of setMax(int)", i + 1, plain.max());
            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong size", 21 - (max - i - 1), bit_fast.size());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong size", 21 - (max - i - 1), bit_small.size());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong size", 21 - (max - i - 1), plain.size());
            for (int j = min; j <= i + 1; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", plain.contains(j));
            }

            for (int j = i + 2; j <= max; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", !bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", !bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", !plain.contains(j));
            }
        }
    }

    public void testSetMinAndCreateUndo() {
        Undo[] bit_fastUndo = new Undo[bit_fast.size()], bit_smallUndo = new Undo[bit_fast.size()],
                plainUndo = new Undo[bit_fast.size()];

        for (int i = -10; i <= 10; i++) {
            bit_fastUndo[i - min] = bit_fast.createUndo();
            bit_fastUndo[i - min].undoable(bit_fast);
            try {
                bit_fast.setMin(i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
            }

            bit_smallUndo[i - min] = bit_small.createUndo();
            bit_smallUndo[i - min].undoable(bit_small);
            try {
                bit_small.setMin(i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
            }

            plainUndo[i - min] = plain.createUndo();
            plainUndo[i - min].undoable(plain);
            try {
                plain.setMin(i);
            } catch (Failure f) {
                fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_PLAIN)");
            }

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of setMin(int)", i, bit_fast.min());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of setMin(int)", i, bit_small.min());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of setMin(int)", i, plain.min());
            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong size", 21 - (i - min), bit_fast.size());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong size", 21 - (i - min), bit_small.size());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong size", 21 - (i - min), plain.size());
            for (int j = min; j < i; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", !bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", !bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", !plain.contains(j));
            }

            for (int j = i; j <= max; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", plain.contains(j));
            }
        }
        assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of bound()", bit_fast.bound());
        assertTrue("IntVarImpl.Domain_BIT_SMALL: wrong work of bound()", bit_small.bound());
        assertTrue("IntVarImpl.Domain_PLAIN: wrong work of contains(int)", plain.bound());
        try {
            assertEquals("IntVar.value() failed", max, bit_fast.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        }
        try {
            assertEquals("IntVar.value() failed", max, bit_small.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        }
        try {
            assertEquals("IntVar.value() failed", max, bit_fast.value());
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_PLAIN)");
        }
        /** **************************************************************************************************** */
        // restoration from undos
        for (int i = 10; i > -10; i--) {
            bit_fastUndo[i - min].undo();
            bit_smallUndo[i - min].undo();
            plainUndo[i - min].undo();

            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong work of setMin(int)", i - 1, bit_fast.min());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong work of setMin(int)", i - 1, bit_small.min());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong work of setMin(int)", i - 1, plain.min());
            assertEquals("IntVarImpl.Domain_BIT_FAST: wrong size", 21 - (i - 1 - min), bit_fast.size());
            assertEquals("IntVarImpl.Domain_BIT_SMALL: wrong size", 21 - (i - 1 - min), bit_small.size());
            assertEquals("IntVarImpl.Domain_PLAIN: wrong size", 21 - (i - 1 - min), plain.size());
            for (int j = min; j < i - 1; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", !bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", !bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", !plain.contains(j));
            }

            for (int j = i - 1; j <= max; j++) {
                assertTrue("IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)", bit_fast.contains(j));
                assertTrue("IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)", bit_small.contains(j));
                assertTrue("IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)", plain.contains(j));
            }
        }
    }
}