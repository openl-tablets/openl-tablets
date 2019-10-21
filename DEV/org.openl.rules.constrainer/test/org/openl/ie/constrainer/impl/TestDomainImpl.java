package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestDomainImpl extends TestCase {
    private Constrainer C = new Constrainer("TestDomainImpl");
    private IntVar _var = C.addIntVar(0, 10, IntVar.DOMAIN_BIT_FAST);
    private DomainImpl _probeDomainImpl = new DomainImpl(_var, _var.min(), _var.max());

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestDomainImpl.class));
    }

    public TestDomainImpl(String name) {
        super(name);
    }

    public void forceSize(int val) {
        DomainImpl db = new DomainImpl(_var, _var.min() + 5, _var.max());
        db.forceSize(125);
        assertEquals(db.size(), 125);
    }

    public void setValue(int value) {
        DomainBits db = new DomainBits(_var, _var.min(), _var.max());
        try {
            assertTrue(!db.setValue(db.max() + 1));
            fail("test failed due to incorrect work of TestDomainImpl.setValue(int)");
        } catch (Failure f) {
        }
        try {
            assertTrue(!db.setValue(db.min() - 1));
            fail("test failed due to incorrect work of TestDomainImpl.setValue(int)");
        } catch (Failure f) {
        }

        try {
            db.setValue(5);
            assertEquals(db.size(), 1);
            assertTrue((db.max() == db.min()) && (db.min() == 5));
        } catch (Failure f) {
            fail("test failed due to incorrect work of TestDomainImpl.setValue(int)");
        }
    }

    public void testContains() {
        int[] goodArray = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] badArray = { -1, -2, 12, 14, 17, 18, 23, 24, 25, -34, 11 };
        for (int i = 0; i < goodArray.length; i++) {
            assertTrue(_probeDomainImpl.contains(goodArray[i]));
            assertTrue(!_probeDomainImpl.contains(badArray[i]));
        }
    }

    public void testIterateDomain() {
        IntVar intvar = C.addIntVar(0, 10);
        DomainImpl db = new DomainImpl(intvar, intvar.min(), intvar.max());
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
        DomainImpl di = new DomainImpl(var, var.min(), var.max());
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

        // intersection is a part of a domain (less than the whole) that does not
        // include
        // neither left nor right end of the domain
        try {
            assertTrue(!di.removeRange(newmin + 1, newmax - 1)); // nothing
            // is to be
            // done
            assertEquals(newmin, di.min());// hasn't changed
            assertEquals(newmax, di.max());// hasn't changed
            assertEquals(newsize, di.size());// hasn't changed
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testRemoveValue() {
        DomainImpl db = new DomainImpl(_var, _var.min(), _var.max());
        int start_size = db.size();
        int start_min = db.min();
        for (int i = 0; i < start_size - 1; i++) {
            try {
                assertTrue(db.removeValue(start_min + i));
            } catch (Failure f) {
                fail("test failed!");
            } catch (Throwable ex) {
                fail("Unexpected exception has been thrown");
            }
            assertTrue(!db.contains(start_min + i));
            assertEquals(start_size - i - 1, db.size());
            assertEquals(db.min(), start_min + i + 1);
        }

        assertEquals(1, db.size());
        try {
            db.removeValue(start_min + start_size - 1);
            fail("test failed");
        } catch (Failure f) {
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
            fail("test of DomainImpl failed due to incorrect work of setMax(int)");
        } catch (Failure f) {
        } catch (Throwable e) {
            fail("Unexpected exception has been thrown!");
        }

        try {
            int oldMax = db.max();
            int oldSize = db.size();
            db.setMax(5);
            assertEquals(5, db.max());
            assertEquals(oldMax - db.max(), oldSize - db.size());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSetMin() {
        DomainBits db = new DomainBits(_var, _var.min(), _var.max());
        try {
            assertTrue(!db.setMin(_var.min() - 1));
        } catch (Failure f) {
            fail("test failed!");
        }
        try {
            db.setMin(_var.max() + 1);
            fail("test of DomainImpl failed due to incorrect work of setMin(int)");
        } catch (Failure f) {
        } catch (Throwable e) {
            fail("Unexpected exception has been thrown!");
        }

        try {
            int oldMin = db.min();
            int oldSize = db.size();
            db.setMin(5);
            assertEquals(5, db.min());
            assertEquals(db.min() - oldMin, oldSize - db.size());
        } catch (Failure f) {
            fail("test failed");
        }
    }
}