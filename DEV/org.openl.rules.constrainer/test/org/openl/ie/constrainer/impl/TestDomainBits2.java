package org.openl.ie.constrainer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;

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

public class TestDomainBits2 {
    private final Constrainer C = new Constrainer("TestDomainBits2");
    private final IntVar _var = C.addIntVar(0, 10, IntVar.DOMAIN_BIT_FAST);
    private final DomainBits2 _probeDomainBits2 = new DomainBits2(_var, _var.min(), _var.max());

    static private int[] bitsToBits2(boolean[] bits) {
        final int BITS_PER_INT = 32;
        int length = bits.length / BITS_PER_INT + 1;
        int[] bits2 = new int[length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < BITS_PER_INT; j++) {
                int idx = i * BITS_PER_INT + j;
                if (idx >= bits.length) {
                    return bits2;
                }
                if (bits[idx]) {
                    bits2[i] |= 1 << j;
                }
            }
        }
        return bits2;
    }

    @Test
    public void forceSize() {
        DomainBits2 db = new DomainBits2(_var, _var.min() + 5, _var.max());
        db.forceSize(125);
        assertEquals(db.size(), 125);
    }

    @Test
    public void setValue() {
        DomainBits2 db = new DomainBits2(_var, _var.min(), _var.max());
        boolean[] mask = new boolean[db.size()];
        Arrays.fill(mask, true);
        mask[5] = false;
        db.forceBits(bitsToBits2(mask));

        try {
            db.setValue(5);
            fail("test failed");
        } catch (Failure ignored) {
        }

        try {
            db.setValue(4);
            assertEquals(db.max(), 4);
            assertEquals(db.min(), 4);
            assertEquals(db.size(), 1);
        } catch (Exception e) {
            fail("test failed due to incorrect work of DomainBits2.setValue(int)");
        }

    }

    @Test
    public void testBits() {
        DomainBits2 db = new DomainBits2(_var, _var.min() + 5, _var.max());
        boolean[] bits = {false, false, true, false, true, true};
        db.forceBits(bitsToBits2(bits));
        int[] bt = db.bits();
        assertEquals(bt.length, 1);
        assertEquals(bt[0], 52);
    }

    @Test
    public void testContains() {
        int[] goodArray = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] badArray = {-1, -2, 12, 14, 17, 18, 23, 24, 25, -34, 11};
        for (int i = 0; i < goodArray.length; i++) {
            assertTrue(_probeDomainBits2.contains(goodArray[i]));
            assertFalse(_probeDomainBits2.contains(badArray[i]));
        }

        IntVar intvar = C.addIntVar(0, 1024);
        DomainBits2 db2 = new DomainBits2(intvar, intvar.min(), intvar.max());
        int[] mask = new int[db2.size() / 32 + 1];
        Arrays.fill(mask, 1);
        db2.forceBits(mask);
        for (int i = 0; i <= db2.size(); i++) {
            if (i % 32 == 0) {
                assertTrue(db2.contains(i));
            } else {
                assertFalse(db2.contains(i));
            }
        }
    }

    @Test
    public void testForceBits() {
        DomainBits2 db = new DomainBits2(_var, _var.min() + 5, _var.max());
        boolean[] bits = new boolean[]{false, true, false, true, false, true};
        db.forceBits(bitsToBits2(bits));
        for (int i = 5; i <= _var.max(); i++) {
            assertEquals(db.contains(i), bits[i - 5], "does not contain " + i);
        }
    }

    @Test
    public void testForceInsert() {
        DomainBits2 db = new DomainBits2(_var, _var.min() + 5, _var.max());
        boolean[] bits = new boolean[]{false, false, false, false, false, false};
        db.forceBits(bitsToBits2(bits));
        db.forceInsert(5);
        db.forceInsert(6);
        db.forceInsert(7);
        db.forceInsert(8);
        for (int i = 5; i <= 8; i++) {
            assertTrue(db.contains(i), "does not contain " + i);
        }
    }

    @Test
    public void testIterateDomain() {
        IntVar intvar = C.addIntVar(0, 10);
        DomainBits2 db = new DomainBits2(intvar, intvar.min(), intvar.max());
        final int[] values = new int[db.size()];
        try {
            db.iterateDomain(new IntExp.IntDomainIterator() {
                private int idx = 0;

                @Override
                public boolean doSomethingOrStop(int val) throws Failure {
                    values[idx++] = val;
                    return true;
                }
            });
            for (int i = intvar.min(); i < intvar.max(); i++) {
                assertEquals(i, values[i]);
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

    @Test
    public void testRemoveRange() {
        IntVar var = C.addIntVar(-10, 10, IntVar.DOMAIN_PLAIN);
        DomainBits di = new DomainBits(var, var.min(), var.max());
        int start_size = di.size();
        int start_min = di.min();
        int start_max = di.max();

        // intersection of range to be removed and the domain is an empty set
        try {
            assertFalse(di.removeRange(start_min - 3, start_min - 1)); // nothing
            // is to
            // be
            // done
        } catch (Failure f) {
            fail("it wouldn't ever happen");
        }
        // intersection of range to be removed and the domain equals to the
        // domain
        try {
            assertTrue(di.removeRange(start_min, start_max));
            fail("test failed");
        } catch (Failure f) {/* everything is ok */
        }
        // intersection is a single value
        try {
            assertTrue(di.removeRange(start_min - 1, start_min));
            assertEquals(start_min + 1, di.min());
            assertEquals(start_size - 1, di.size());
        } catch (Failure f) {
            fail("test failed");
        }
        // intersection is a part of a domain (less than the whole) including
        // left end
        int newmin = start_min + (start_max - start_min) / 2 + 1;
        int newsize = start_size - (start_max - start_min) / 2 - 1;
        try {
            assertTrue(di.removeRange(start_min - 10, start_min + (start_max - start_min) / 2));
            assertEquals(newmin, di.min());
            assertEquals(start_max, di.max());// hasn't changed
            assertEquals(newsize, di.size());
        } catch (Failure f) {
            fail("test failed");
        }
        // intersection is a part of a domain (less than the whole) including
        // right end
        int newmax = start_max - (start_max - newmin) / 2 - 1;
        newsize = newsize - (start_max - newmin) / 2 - 1;
        try {
            assertTrue(di.removeRange(start_max - (start_max - newmin) / 2, start_max + 10));
            assertEquals(newmin, di.min());// hasn't changed
            assertEquals(newmax, di.max());
            assertEquals(newsize, di.size());
        } catch (Failure f) {
            fail("test failed");
        }

        // intersection is a part of a domain (less than the whole) that does not
        // include
        // neither left nor right end of the domain
        newsize = newsize - (newmax - 1 - (newmin + 1) + 1);
        try {
            assertTrue(di.removeRange(newmin + 1, newmax - 1));
            assertEquals(newmin, di.min());// hasn't changed
            assertEquals(newmax, di.max());// hasn't changed
            assertEquals(newsize, di.size());
            for (int i = di.min(); i < newmin + 1; i++) {
                assertTrue(di.contains(i));
            }
            for (int i = di.max(); i > newmax - 1; i--) {
                assertTrue(di.contains(i));
            }
            for (int i = newmin + 1; i < newmax - 1; i++) {
                assertFalse(di.contains(i));
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

    @Test
    public void testRemoveValue() {
        DomainBits2 db = new DomainBits2(_var, _var.min(), _var.max());
        int start_size = db.size();
        int start_min = db.min();
        for (int i = start_min; i < db.max(); i++) {
            try {
                assertTrue(db.removeValue(i));
            } catch (Failure f) {
                fail("test failed.");
            } catch (Exception ex) {
                fail("Unexpected exception has been thrown.");
            }
            assertFalse(db.contains(i));
            assertEquals(start_size - (i - start_min + 1), db.size());
            assertEquals(i + 1, db.min());
        }
    }

    @Test
    public void testSetMax() {
        DomainBits2 db = new DomainBits2(_var, _var.min(), _var.max());
        try {
            assertFalse(db.setMax(_var.max() + 1));
        } catch (Failure f) {
            fail("test failed.");
        }
        try {
            db.setMax(_var.min() - 1);
            fail("test of DomainBits2 failed due to incorrect work of setMax(int)");
        } catch (Failure ignored) {
        } catch (Exception e) {
            fail("Unexpected exception has been thrown.");
        }
        db.size();
        db.forceBits(
                bitsToBits2(new boolean[]{true, true, true, true, true, false, false, true, false, true, true}));
        try {
            db.setMax(8);
            assertEquals(7, db.max());
            assertEquals(9, db.size());
        } catch (Failure f) {
            fail("test failed.");
        }
    }
}