package org.openl.ie.constrainer.impl;

import java.util.HashMap;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.impl.FloatExpOpposite;

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

public class TestFloatExpOpposite extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpOpposite");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpOpposite.class));
    }

    public TestFloatExpOpposite(String name) {
        super(name);
    }

    public void testAscendingEventPropagation() {
        FloatVar floatVar = C.addFloatVar(-5, 3, "");
        FloatExp floatExp = new FloatExpOpposite(floatVar);
        assertEquals(5, floatExp.max(), Constrainer.precision());
        assertEquals(-3, floatExp.min(), Constrainer.precision());
        try {
            floatVar.setMin(-1);
            floatVar.setMax(2);
            C.propagate();
            assertEquals(1, floatExp.max(), Constrainer.precision());
            assertEquals(-2, floatExp.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testCalcCoeffs() {
        FloatVar floatVar = C.addFloatVar(-5, 3, "");
        FloatExp floatExp = new FloatExpOpposite(floatVar);
        HashMap map = new HashMap();
        try {
            assertEquals(0, (int) floatExp.calcCoeffs(map));
            assertEquals(1, map.size());
            assertEquals(-1, (int) ((Double) (map.get(floatVar))).doubleValue());
        } catch (NonLinearExpression ex) {
            fail("FloatExpOpposite treated as non linear expression");
        }
    }

    public void testConstraintsPosting() {
        FloatVar floatVar = C.addFloatVar(-5, 3, "");
        FloatExp floatExp = new FloatExpOpposite(floatVar);
        try {
            C.postConstraint(floatExp.le(2));
            floatVar.setMax(-2 - 2 * Constrainer.precision());
            C.propagate();
            fail("test failed");
        } catch (Failure f) {
        }
    }

    public void testMinMax() {
        // max < 0;
        FloatVar floatVar = C.addFloatVar(-5, -1, "");
        FloatExp floatExp = new FloatExpOpposite(floatVar);
        assertEquals(5, floatExp.max(), Constrainer.precision());
        assertEquals(1, floatExp.min(), Constrainer.precision());

        // max = 0
        floatVar = C.addFloatVar(-5, 0, "");
        floatExp = new FloatExpOpposite(floatVar);
        assertEquals(5, floatExp.max(), Constrainer.precision());
        assertEquals(0, floatExp.min(), Constrainer.precision());

        // min < 0 < max
        floatVar = C.addFloatVar(-5, 2, "");
        floatExp = new FloatExpOpposite(floatVar);
        assertEquals(-2, floatExp.min(), Constrainer.precision());
        assertEquals(5, floatExp.max(), Constrainer.precision());

        // min > 0
        floatVar = C.addFloatVar(2, 4, "");
        floatExp = new FloatExpOpposite(floatVar);
        assertEquals(-4, floatExp.min(), Constrainer.precision());
        assertEquals(-2, floatExp.max(), Constrainer.precision());
    }
}