package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.impl.FloatExpAddArray;

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
 * @author unascribed
 * @version 1.0
 */

public class TestFloatExpAddArray extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpAddArray");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpAddArray.class));
    }

    public TestFloatExpAddArray(String name) {
        super(name);
    }

    private double accumulate(double[] arr) {
        double sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum;
    }

    public void testMinMax() {
        double[] upperBounds = new double[100], lowerBounds = new double[upperBounds.length];

        FloatExpArray array = new FloatExpArray(C, upperBounds.length);
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < upperBounds.length; j++) {
                upperBounds[j] = (Math.random() * 2 - 1) * 100;
                lowerBounds[j] = upperBounds[j] - Math.random() * 100;
                array.set(C.addFloatVar(lowerBounds[j], upperBounds[j], ""), j);
            }
            FloatExpAddArray arraySum = new FloatExpAddArray(C, array);
            assertEquals(accumulate(upperBounds), arraySum.max(), Constrainer.precision());
            assertEquals(accumulate(lowerBounds), arraySum.min(), Constrainer.precision());
        }
    }

    public void testSetMinSetMax() {
        FloatExpArray array = null;
        FloatExpAddArray arraySum = null;
        // ascending eventpropagation
        // initialization block
        array = new FloatExpArray(C, 10);
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addFloatVar(0, 9, ""), i);
        }
        arraySum = new FloatExpAddArray(C, array);
        assertEquals(10 * 9, arraySum.max(), Constrainer.precision());
        assertEquals(0, arraySum.min(), Constrainer.precision());

        // fire max event
        for (int i = 0; i < array.size(); i++) {
            try {
                FloatExp floatVar = array.get(i);
                floatVar.setMax(9 - Constrainer.precision() / 2);
                C.propagate();
                // Nothing is to change
                assertEquals(10 * 9 - i * 2 * Constrainer.precision(), arraySum.max(), Constrainer.precision());
                assertEquals(0 + i * 2 * Constrainer.precision(), arraySum.min(), Constrainer.precision());

                floatVar.setMax(9 - 2 * Constrainer.precision());
                C.propagate();
                // Max is to change
                assertEquals(10 * 9 - (i + 1) * 2 * Constrainer.precision(), arraySum.max(), Constrainer.precision());
                assertEquals(0 + i * 2 * Constrainer.precision(), arraySum.min(), Constrainer.precision());

                floatVar.setMin(Constrainer.precision() / 2);
                C.propagate();
                // Nothing is to change
                assertEquals(10 * 9 - (i + 1) * 2 * Constrainer.precision(), arraySum.max(), Constrainer.precision());
                assertEquals(0 + i * 2 * Constrainer.precision(), arraySum.min(), Constrainer.precision());

                floatVar.setMin(0 + 2 * Constrainer.precision());
                C.propagate();
                // Max is to change
                assertEquals(10 * 9 - (i + 1) * 2 * Constrainer.precision(), arraySum.max(), Constrainer.precision());
                assertEquals(0 + (i + 1) * 2 * Constrainer.precision(), arraySum.min(), Constrainer.precision());
            } catch (Failure f) {
                fail("test failed due to incorrect work of Floatvar.setMin(double) or Floatvar.setMax(double)");
            }
        }

        // constraint satisfaction
        // initialization block
        array = new FloatExpArray(C, 10);
        for (int i = 0; i < array.size(); i++) {
            array.set(C.addFloatVar(0, 9, ""), i);
        }
        arraySum = new FloatExpAddArray(C, array);
        assertEquals(10 * 9, arraySum.max(), Constrainer.precision());
        assertEquals(0, arraySum.min(), Constrainer.precision());
        try {
            C.postConstraint(arraySum.gt(10 * 9 - 5));
        } catch (Failure f) {
            fail("test failed");
        }

        try {
            double oldmax = array.get(2).max();
            array.get(2).setMax(oldmax - 5);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatVar.setMax(double)");
        }

        for (int i = 0; i < array.size(); i++) {
            try {
                double oldmax = array.get(2).max();
                ;
                array.get(i).setMax(oldmax - 5 - 2 * Constrainer.precision());
                C.propagate();
                fail("test failed: constraint satisfaction");
            } catch (Failure f) {/* that's ok */
            }
        }
    }

}