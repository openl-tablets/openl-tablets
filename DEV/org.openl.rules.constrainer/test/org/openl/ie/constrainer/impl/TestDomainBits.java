package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.DomainBits;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestDomainBits extends TestCase {
    private Constrainer C = new Constrainer("TestDomainBits");
    private IntVar _var = C.addIntVar(0, 10, IntVar.DOMAIN_BIT_FAST);
    private DomainBits _probeDomainBits = new DomainBits(_var, _var.min(), _var.max());

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestDomainBits.class));
    }

    public TestDomainBits(String name) {
        super(name);
    }

    public void forceSize(int val) {
        DomainBits db = new DomainBits(_var, _var.min() + 5, _var.max());
        db.forceSize(125);
        assertEquals(db.size(), 125);
    }

    public void setValue(int value) {
        DomainBits db = new DomainBits(_var, _var.min(), _var.max());
        boolean[] mask = new boolean[db.size()];
        for (int i = 0; i < mask.length; i++) {
            mask[i] = true;
        }
        mask[5] = false;
        db.forceBits(mask);

        try {
            db.setValue(5);
            fail("test failed");
        } catch (Failure e) {
        }

        try {
            db.setValue(4);
            assertEquals(db.max(), 4);
            assertEquals(db.min(), 4);
            assertEquals(db.size(), 1);
        } catch (Exception e) {
            fail("test failed due to incorrect work of DomainBits.setValue(int)");
        }

    }

    public void testBits() {
        DomainBits db = new DomainBits(_var, _var.min() + 5, _var.max());
        boolean[] bits = { false, false, true, false, true, true };
        db.forceBits(bits);
        boolean[] bt = db.bits();
        for (int i = 0; i < bits.length; i++) {
            assertTrue(bits[i] == bt[i]);
        }
    }

    public void testContains() {
        int[] goodArray = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] badArray = { -1, -2, 12, 14, 17, 18, 23, 24, 25, -34, 11 };
        for (int i = 0; i < goodArray.length; i++) {
            assertTrue(_probeDomainBits.contains(goodArray[i]));
            assertTrue(!_probeDomainBits.contains(badArray[i]));
        }
    }

    public void testForceBits() {
        DomainBits db = new DomainBits(_var, _var.min() + 5, _var.max());
        boolean[] bits = new boolean[] { false, true, false, true, false, true };
        db.forceBits(bits);
        for (int i = 5; i <= _var.max(); i++) {
            assertTrue("doesn't contain " + i, db.contains(i) == bits[i - 5]);
        }
    }

    public void testForceInsert() {
        DomainBits db = new DomainBits(_var, _var.min() + 5, _var.max());
        boolean[] bits = new boolean[] { false, false, false, false, false, false };
        db.forceBits(bits);
        db.forceInsert(5);
        db.forceInsert(6);
        db.forceInsert(7);
        db.forceInsert(8);
        for (int i = 5; i <= 8; i++) {
            assertTrue("doesn't contain " + i, db.contains(i));
        }
    }

    public void testIterateDomain() {
        IntVar intvar = C.addIntVar(0, 10);
        DomainBits db = new DomainBits(intvar, intvar.min(), intvar.max());
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

    public void testRemoveRange() {
        IntVar var = C.addIntVar(-10, 10, IntVar.DOMAIN_PLAIN);
        DomainBits di = new DomainBits(var, var.min(), var.max());
        int start_size = di.size();
        int start_min = di.min();
        int start_max = di.max();

        // intersection of range to be removed and the domain is an empty set
        try {
            assertTrue(!di.removeRange(start_min - 3, start_min - 1)); // nothing
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

        // intersection is a part of a domain (less than the whole) that doesn't
        // include
        // neither left nor right end of the domain
        newsize = newsize - ((newmax - 1) - (newmin + 1) + 1);
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
                assertTrue(!di.contains(i));
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testRemoveValue() {
        DomainBits db = new DomainBits(_var, _var.min(), _var.max());
        int start_size = db.size();
        int start_min = db.min();
        for (int i = start_min; i < db.max(); i++) {
            try {
                assertTrue(db.removeValue(i));
            } catch (Failure f) {
                fail("test failed!");
            } catch (Throwable ex) {
                fail("Unexpected exception hasa been thrown");
            }
            assertTrue(!db.contains(i));
            assertEquals(start_size - (i - start_min + 1), db.size());
            assertEquals(i + 1, db.min());
        }
    }

    public void testSetMax() {
        DomainBits db = new DomainBits(_var, _var.min(), _var.max());
        try {
            assertTrue(!db.setMax(_var.max() + 1));
        } catch (Failure f) {
            fail("test failed!");
        }
        try {
            db.setMax(_var.min() - 1);
            fail("test of DomainBits failed due to incorrect work of setMax(int)");
        } catch (Failure f) {
        } catch (Throwable e) {
            fail("Unexpected exception has been thrown!");
        }
        boolean[] oldbits = db.bits();
        boolean[] newbits = new boolean[db.size()];
        db.forceBits(new boolean[] { true, true, true, true, true, false, false, true, false, true, true });
        try {
            db.setMax(8);
            assertEquals(7, db.max());
            assertEquals(9, db.size());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testSetMin(int m) {
        DomainBits db = new DomainBits(_var, _var.min(), _var.max());
        try {
            assertTrue(!db.setMin(_var.max() + 1));
        } catch (Failure f) {
            fail("test failed!");
        }
        try {
            db.setMin(_var.min() - 1);
            fail("test of DomainBits failed due to incorrect work of setMax(int)");
        } catch (Failure f) {
        } catch (Throwable e) {
            fail("Unexpected exception has been thrown!");
        }
        boolean[] oldbits = db.bits();
        boolean[] newbits = new boolean[db.size()];
        db.forceBits(new boolean[] { true, false, true, true, true, true, false, true, true, true, true });
        try {
            db.setMin(6);
            assertEquals(7, db.min());
            assertEquals(4, db.size());
        } catch (Failure f) {
            fail("test failed!");
        }

    }

}