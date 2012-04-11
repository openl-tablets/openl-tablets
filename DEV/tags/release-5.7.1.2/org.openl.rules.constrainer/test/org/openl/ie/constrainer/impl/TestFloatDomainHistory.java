package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.impl.FloatDomainHistory;
import org.openl.ie.constrainer.impl.FloatVarImpl;

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

public class TestFloatDomainHistory extends TestCase {
    private Constrainer C = new Constrainer("TestFloatDomainHistory");
    private double delta = Constrainer.FLOAT_MIN;

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatDomainHistory.class));
    }

    public TestFloatDomainHistory(String name) {
        super(name);
    }

    public void testSaveRestore() {
        FloatVarImpl floatVar = null;
        try {
            floatVar = (FloatVarImpl) C.addFloatVar(-Math.PI * 100, Math.PI * 100);
        } catch (Failure f) {
            fail("test failed: can't create a variable of type FloatVar");
        }
        double min = floatVar.min();
        double max = floatVar.max();

        FloatDomainHistory history = floatVar.history();
        int[] indices = new int[10];

        history.save();
        indices[0] = history.currentIndex();

        for (int i = 1; i < 10; i++) {
            try {
                floatVar.setMin(min + i * 10 * Math.PI);
                floatVar.setMax(max - i * 10 * Math.PI);
            } catch (Failure f) {
                fail("test failed due to incorrect behaviour of FloatVar.setMin(double)");
            }
            assertEquals(min + (i - 1) * 10 * Math.PI, history.oldmin(), delta);
            assertEquals(max - (i - 1) * 10 * Math.PI, history.oldmax(), delta);
            history.save();
            indices[i] = history.currentIndex();
        }

        // restoration in descending order
        for (int i = 9; i >= 0; i--) {
            history.restore(indices[i]);
            assertEquals(min + i * 10 * Math.PI, floatVar.min(), delta);
            assertEquals(max - i * 10 * Math.PI, floatVar.max(), delta);
        }

        // restoration in ascending order
        for (int i = 1; i <= 9; i++) {
            history.restore(indices[i]);
            assertEquals(min + i * 10 * Math.PI, floatVar.min(), delta);
            assertEquals(max - i * 10 * Math.PI, floatVar.max(), delta);
        }
    }

    public void testSetMaxSetMin() {
        FloatVarImpl floatVar = null;
        try {
            floatVar = (FloatVarImpl) C.addFloatVar(-20, 20);
        } catch (Failure f) {
            fail("test failed: can't create a variable of type FloatVar");
        }
        double min = floatVar.min();
        double max = floatVar.max();

        FloatDomainHistory history = floatVar.history();
        try {
            floatVar.setMin(-10);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatVar.setMin(double)");
        }
        assertTrue((history._mask & EventOfInterest.MIN) > 0);
        assertEquals(0, history._mask & EventOfInterest.VALUE);

        try {
            floatVar.setMax(10);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatVar.setMax(double)");
        }
        assertTrue((history._mask & EventOfInterest.MAX) > 0);
        assertEquals(0, history._mask & EventOfInterest.VALUE);

        try {
            floatVar.setMin(10);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatVar.setMin(double)");
        }
        assertTrue((history._mask & EventOfInterest.MIN) > 0);
        assertTrue("No", (history._mask & EventOfInterest.VALUE) > 0);
    }

}