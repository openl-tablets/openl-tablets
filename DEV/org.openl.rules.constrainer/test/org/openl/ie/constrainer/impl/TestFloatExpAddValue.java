package org.openl.ie.constrainer.impl;

import java.util.HashMap;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.impl.FloatExpAddValue;

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

public class TestFloatExpAddValue extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpAddValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpAddValue.class));
    }

    public TestFloatExpAddValue(String name) {
        super(name);
    }

    public void testAscendantEventPropagation() {
        FloatVar floatVar1 = C.addFloatVar(0, 10, "");

        FloatExp floatExp = new FloatExpAddValue(floatVar1, 5);

        try {
            floatVar1.setMax(5);
            C.propagate();
            assertEquals(10, floatExp.max(), Constrainer.precision());

            floatVar1.setMin(2);
            C.propagate();
            assertEquals(7, floatExp.min(), Constrainer.precision());

        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testCalcCoeffs() {
        FloatVar floatVar1 = C.addFloatVar(0, 10, "");
        FloatExp floatExp = new FloatExpAddValue(floatVar1, 5);
        HashMap map = new HashMap();
        try {
            assertEquals(5, (int) floatExp.calcCoeffs(map));
        } catch (NonLinearExpression e) {
            fail("FloatExpAddValue treated as NonLinearExpression");
        }
        assertEquals(1, map.size());
        assertEquals(1, (int) ((Double) (map.get(floatVar1))).doubleValue());
    }

    public void testSetMinSetMax() {
        FloatVar floatVar1 = C.addFloatVar(0, 10, "");
        FloatExp addExp = new FloatExpAddValue(floatVar1, 5);

        // descendant event propagation : setMax()
        try {
            assertEquals(15, addExp.max(), Constrainer.precision());
            assertEquals(5, addExp.min(), Constrainer.precision());
            addExp.setMax(9);
            C.propagate();
            assertEquals(4, floatVar1.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed due to incorrect work of FloatVar.setMin(double)");
        }

        // constraint propagation
        floatVar1 = C.addFloatVar(0, 10, "");
        addExp = new FloatExpAddValue(floatVar1, 5);
        try {
            C.postConstraint(addExp.le(10));
            C.propagate();
            assertEquals(5, floatVar1.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed: Constraint propagation");
        }
    }
}