package org.openl.ie.constrainer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Undo;

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

public class TestIntVarImpl {
    private final Constrainer C = new Constrainer("TestIntVarImpl");
    private final int size = 21;
    private final int min = -10;
    private final int max = 10;
    private final IntVar bit_fast = new IntVarImpl(C, min, max, "intvar1", IntVar.DOMAIN_BIT_FAST);
    private final IntVar bit_small = new IntVarImpl(C, min, max, "intvar2", IntVar.DOMAIN_BIT_SMALL);
    private final IntVar plain = new IntVarImpl(C, min, max, "intvar3", IntVar.DOMAIN_PLAIN);

    @Test
    public void testA() {
        new IntVarImpl(C, 0, 10, "intvar", IntVar.DOMAIN_BIT_FAST);

    }

    @Test
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

    @Test
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

            assertEquals(newMin, bit_fast.min(), "IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)");
            assertEquals(newMin, bit_small.min(), "IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)");
            assertEquals(newMin, plain.min(), "IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)");
            assertEquals(newMax, bit_fast.max(), "IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)");
            assertEquals(newMax, bit_small.max(), "IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)");
            assertEquals(newMax, plain.max(), "IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)");

            assertEquals(curSize, bit_fast.size(), "IntVarImpl.Domain_BIT_FAST: wrong size");
            assertEquals(curSize, bit_small.size(), "IntVarImpl.Domain_BIT_SMALL: wrong size");
            assertEquals(curSize, plain.size(), "IntVarImpl.Domain_PLAIN: wrong size");
            for (int j = newMin; j <= newMax; j++) {
                assertTrue(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertTrue(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertTrue(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }

            for (int j = min; j < newMin; j++) {
                assertFalse(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertFalse(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertFalse(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }

            for (int j = newMax + 1; j <= max; j++) {
                assertFalse(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertFalse(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertFalse(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }
            counter++;
        }

        try {
            bit_fast.removeValue(0);
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        } catch (Failure ignored) {
        }

        try {
            bit_small.removeValue(0);
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        } catch (Failure ignored) {
        }

        try {
            plain.removeValue(0);
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_PLAIN)");
        } catch (Failure ignored) {
        }

        assertTrue(bit_fast.bound(), "IntVarImpl.Domain_BIT_FAST: wrong work of bound()");
        assertTrue(bit_small.bound(), "IntVarImpl.Domain_BIT_SMALL: wrong work of bound()");
        assertTrue(plain.bound(), "IntVarImpl.Domain_PLAIN: wrong work of contains(int)");
        try {
            assertEquals(0, bit_fast.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        }
        try {
            assertEquals(0, bit_small.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.removeValue(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        }
        try {
            assertEquals(0, bit_fast.value(), "IntVar.value() failed");
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

            assertEquals(curMin, bit_fast.min(), "IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)");
            assertEquals(curMin, bit_small.min(), "IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)");
            assertEquals(curMin, plain.min(), "IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)");

            assertEquals(curMax, bit_fast.max(), "IntVarImpl.Domain_BIT_FAST: wrong work of removeValue(int)");
            assertEquals(curMax, bit_small.max(), "IntVarImpl.Domain_BIT_SMALL: wrong work of removeValue(int)");
            assertEquals(curMax, plain.max(), "IntVarImpl.Domain_PLAIN: wrong work of removeValue(int)");

            assertEquals(curSize, bit_fast.size(), "IntVarImpl.Domain_BIT_FAST: wrong size");
            assertEquals(curSize, bit_small.size(), "IntVarImpl.Domain_BIT_SMALL: wrong size");
            assertEquals(curSize, plain.size(), "IntVarImpl.Domain_PLAIN: wrong size");
            for (int j = curMin; j <= curMax; j++) {
                assertTrue(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertTrue(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertTrue(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }

            for (int j = min; j < curMin; j++) {
                assertFalse(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertFalse(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertFalse(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }
            counter++;
        }
    }

    @Test
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

            assertEquals(i, bit_fast.max(), "IntVarImpl.Domain_BIT_FAST: wrong work of setMax(int)");
            assertEquals(i, bit_small.max(), "IntVarImpl.Domain_BIT_SMALL: wrong work of setMax(int)");
            assertEquals(i, plain.max(), "IntVarImpl.Domain_PLAIN: wrong work of setMax(int)");
            assertEquals(21 - (max - i), bit_fast.size(), "IntVarImpl.Domain_BIT_FAST: wrong size");
            assertEquals(21 - (max - i), bit_small.size(), "IntVarImpl.Domain_BIT_SMALL: wrong size");
            assertEquals(21 - (max - i), plain.size(), "IntVarImpl.Domain_PLAIN: wrong size");
            for (int j = max; j > i; j--) {
                assertFalse(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertFalse(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertFalse(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }

            for (int j = i; j >= min; j--) {
                assertTrue(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertTrue(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertTrue(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }
        }

        assertTrue(bit_fast.bound(), "IntVarImpl.Domain_BIT_FAST: wrong work of bound()");
        assertTrue(bit_small.bound(), "IntVarImpl.Domain_BIT_SMALL: wrong work of bound()");
        assertTrue(plain.bound(), "IntVarImpl.Domain_PLAIN: wrong work of contains(int)");
        try {
            assertEquals(min, bit_fast.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        }
        try {
            assertEquals(min, bit_small.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        }
        try {
            assertEquals(min, bit_fast.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMax(int) (Domain type: IntVar.DOMAIN_BIT_PLAIN)");
        }
        /** **************************************************************************************************** */
        // restoration from undos
        for (int i = -10; i < 10; i++) {
            bit_fastUndo[max - i].undo();
            bit_smallUndo[max - i].undo();
            plainUndo[max - i].undo();

            assertEquals(i + 1, bit_fast.max(), "IntVarImpl.Domain_BIT_FAST: wrong work of setMax(int)");
            assertEquals(i + 1, bit_small.max(), "IntVarImpl.Domain_BIT_SMALL: wrong work of setMax(int)");
            assertEquals(i + 1, plain.max(), "IntVarImpl.Domain_PLAIN: wrong work of setMax(int)");
            assertEquals(21 - (max - i - 1), bit_fast.size(), "IntVarImpl.Domain_BIT_FAST: wrong size");
            assertEquals(21 - (max - i - 1), bit_small.size(), "IntVarImpl.Domain_BIT_SMALL: wrong size");
            assertEquals(21 - (max - i - 1), plain.size(), "IntVarImpl.Domain_PLAIN: wrong size");
            for (int j = min; j <= i + 1; j++) {
                assertTrue(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertTrue(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertTrue(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }

            for (int j = i + 2; j <= max; j++) {
                assertFalse(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertFalse(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertFalse(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }
        }
    }

    @Test
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

            assertEquals(i, bit_fast.min(), "IntVarImpl.Domain_BIT_FAST: wrong work of setMin(int)");
            assertEquals(i, bit_small.min(), "IntVarImpl.Domain_BIT_SMALL: wrong work of setMin(int)");
            assertEquals(i, plain.min(), "IntVarImpl.Domain_PLAIN: wrong work of setMin(int)");
            assertEquals(21 - (i - min), bit_fast.size(), "IntVarImpl.Domain_BIT_FAST: wrong size");
            assertEquals(21 - (i - min), bit_small.size(), "IntVarImpl.Domain_BIT_SMALL: wrong size");
            assertEquals(21 - (i - min), plain.size(), "IntVarImpl.Domain_PLAIN: wrong size");
            for (int j = min; j < i; j++) {
                assertFalse(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertFalse(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertFalse(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }

            for (int j = i; j <= max; j++) {
                assertTrue(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertTrue(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertTrue(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }
        }
        assertTrue(bit_fast.bound(), "IntVarImpl.Domain_BIT_FAST: wrong work of bound()");
        assertTrue(bit_small.bound(), "IntVarImpl.Domain_BIT_SMALL: wrong work of bound()");
        assertTrue(plain.bound(), "IntVarImpl.Domain_PLAIN: wrong work of contains(int)");
        try {
            assertEquals(max, bit_fast.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_FAST)");
        }
        try {
            assertEquals(max, bit_small.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_SMALL)");
        }
        try {
            assertEquals(max, bit_fast.value(), "IntVar.value() failed");
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.setMin(int) (Domain type: IntVar.DOMAIN_BIT_PLAIN)");
        }
        /** **************************************************************************************************** */
        // restoration from undos
        for (int i = 10; i > -10; i--) {
            bit_fastUndo[i - min].undo();
            bit_smallUndo[i - min].undo();
            plainUndo[i - min].undo();

            assertEquals(i - 1, bit_fast.min(), "IntVarImpl.Domain_BIT_FAST: wrong work of setMin(int)");
            assertEquals(i - 1, bit_small.min(), "IntVarImpl.Domain_BIT_SMALL: wrong work of setMin(int)");
            assertEquals(i - 1, plain.min(), "IntVarImpl.Domain_PLAIN: wrong work of setMin(int)");
            assertEquals(21 - (i - 1 - min), bit_fast.size(), "IntVarImpl.Domain_BIT_FAST: wrong size");
            assertEquals(21 - (i - 1 - min), bit_small.size(), "IntVarImpl.Domain_BIT_SMALL: wrong size");
            assertEquals(21 - (i - 1 - min), plain.size(), "IntVarImpl.Domain_PLAIN: wrong size");
            for (int j = min; j < i - 1; j++) {
                assertFalse(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertFalse(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertFalse(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }

            for (int j = i - 1; j <= max; j++) {
                assertTrue(bit_fast.contains(j), "IntVarImpl.Domain_BIT_FAST: wrong work of contains(int)");
                assertTrue(bit_small.contains(j), "IntVarImpl.DOMAIN_BIT_SMALL: wrong work of contains(int)");
                assertTrue(plain.contains(j), "IntVarImpl.DOMAIN_PLAIN: wrong work of contains(int)");
            }
        }
    }
}