package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;

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

public class TestIntDomainHistory extends TestCase {
    private Constrainer C = new Constrainer("TestIntDomainHistory");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntDomainHistory.class));
    }

    public TestIntDomainHistory(String name) {
        super(name);
    }

    public void testNumberOfRemoves() {
        IntVar intvar = C.addIntVar(0, 100, IntVar.DOMAIN_BIT_SMALL);
        IntDomainHistory history = new IntDomainHistory(intvar);
        for (int i = intvar.min() + 1; i < intvar.max(); i++) {
            history.remove(i);
        }
        assertEquals(intvar.size() - 2, history.numberOfRemoves());
    }

    public void testOldMinAndOldMax() {
        IntVar intvar = C.addIntVar(0, 100, IntVar.DOMAIN_BIT_SMALL);
        IntDomainHistory history = new IntDomainHistory(intvar);
        int oldmin = intvar.min();
        int oldmax = intvar.max();
        try {
            intvar.setMax(90);
            intvar.setMin(10);
        } catch (Failure f) {
            fail("test failed due to incorrect work of IntVar.SetMax(int)");
        }
        assertEquals(oldmin, history.oldmin());
        assertEquals(oldmax, history.oldmax());
    }

    public void testRemoveInterval() {
        IntVarImpl intvar = (IntVarImpl) C.addIntVar(-10, 10, "intvar1", IntVar.DOMAIN_BIT_FAST);
        IntDomainHistory history = intvar.history();
        new IntDomainHistory(intvar);

        int[] minvals = new int[3];
        minvals[0] = intvar.min();
        int[] maxvals = new int[3];
        maxvals[0] = intvar.max();
        int[] sizes = new int[3];
        sizes[0] = intvar.size();
        int[] removeStart = new int[3];
        removeStart[0] = intvar.max() + 1;
        int[] removeEnd = new int[3];
        removeEnd[0] = intvar.max() + 1;

        int[] indicies = new int[3];

        try {
            history.save();
            indicies[0] = history.currentIndex();

            removeStart[1] = -5;
            removeEnd[1] = 5;
            intvar.removeRange(-5, 5);
            minvals[1] = intvar.min();
            maxvals[1] = intvar.max();
            assertEquals(10, intvar.size());
            sizes[1] = intvar.size();
            history.save();
            indicies[1] = history.currentIndex();

            removeStart[2] = -10;
            removeEnd[2] = 7;
            intvar.removeRange(-10, 7);
            assertEquals(8, intvar.min());
            minvals[2] = intvar.min();
            maxvals[2] = intvar.max();
            assertEquals(3, intvar.size());
            sizes[2] = intvar.size();
            history.save();
            indicies[2] = history.currentIndex();

            for (int j = 2; j >= 0; j--) {
                history.restore(indicies[j]);
                assertEquals(minvals[j], intvar.min());
                assertEquals(maxvals[j], intvar.max());
                assertEquals(sizes[j], intvar.size());
                for (int i = minvals[j]; i < removeStart[j]; i++) {
                    assertTrue("code1:" + i + ": " + j, intvar.contains(i));
                }
                for (int i = maxvals[j]; i > removeEnd[j]; i--) {
                    assertTrue("code2:" + i + ": " + j, intvar.contains(i));
                }
                for (int i = removeStart[j]; i <= removeEnd[j]; i++) {
                    assertTrue("code3:" + i + ": " + j, !intvar.contains(i));
                }
            }
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testSaveRestore() {
        IntVarImpl intvar = (IntVarImpl) C.addIntVar(0, 100, "IntVar1", IntVar.DOMAIN_BIT_FAST);
        IntDomainHistory history = intvar.history();
        new IntDomainHistory(intvar);

        int[] maxVals = { intvar.max(), 95, 90, 85, 80, 75, 70, 65, 60 };
        int[] minVals = { intvar.min(), 5, 10, 15, 25, 30, 35, 40, 45 };
        int[] indices = new int[maxVals.length];
        int[] sizes = new int[maxVals.length];
        sizes[0] = intvar.size();
        indices[0] = history.currentIndex();

        try {
            for (int i = 1; i < maxVals.length; i++) {

                intvar.setMax(maxVals[i]);
                intvar.setMin(minVals[i]);
                assertTrue("incorrect work of IntVar.setMax(int)", intvar.max() == maxVals[i]);
                assertTrue("incorrect work of IntVar.setMin(int)", intvar.min() == minVals[i]);
                sizes[i] = intvar.size();

                // save current state
                history.save();
                indices[i] = history.currentIndex();

                intvar.removeValue(maxVals[i]);
                intvar.removeValue(minVals[i]);
            }
            for (int i = 0; i < maxVals.length - 1; i++) {
                history.restore(indices[i]);
                assertEquals("wrong domain size after restoration", intvar.size(), sizes[i]);
                assertEquals("wrong maximum after restoration", intvar.max(), maxVals[i]);
                assertEquals("wrong minimum after restoration", intvar.min(), minVals[i]);
                assertTrue(intvar.contains(maxVals[i]) && intvar.contains(minVals[i]));
                for (int j = maxVals.length - 1; j > i; j--) {
                    // ensure that all removed values are available after
                    // restoration
                    assertTrue((intvar.contains(maxVals[j])) && (intvar.contains(minVals[i])));
                }
            }
        } catch (Failure f) {
            fail("test failed.");
        }
    }

}