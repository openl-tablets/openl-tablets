package org.openl.ie.constrainer.impl;

import java.util.HashMap;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.impl.FloatExpAddExp;

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

public class TestFloatExpAddExp extends TestCase {
    private Constrainer C = new Constrainer("TestFloatExpAddExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatExpAddExp.class));
    }

    public TestFloatExpAddExp(String name) {
        super(name);
    }

    public void testAscendantEventPropagation() {
        FloatVar floatVar1 = C.addFloatVar(0, 10, ""), floatVar2 = C.addFloatVar(0, 10, "");
        FloatExp floatExp = new FloatExpAddExp(floatVar1, floatVar2);

        try {
            floatVar1.setMax(5);
            C.propagate();
            assertEquals(15, floatExp.max(), Constrainer.precision());

            floatVar2.setMax(5);
            C.propagate();
            assertEquals(10, floatExp.max(), Constrainer.precision());

            floatVar2.setMin(2);
            C.propagate();
            assertEquals(2, floatExp.min(), Constrainer.precision());

            floatVar1.setMin(2);
            C.propagate();
            assertEquals(4, floatExp.min(), Constrainer.precision());

        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testCalcCoeffs() {
        FloatVar floatVar1 = C.addFloatVar(0, 10, ""), floatVar2 = C.addFloatVar(0, 10, "");
        FloatExp floatExp = new FloatExpAddExp(floatVar1, floatVar2);
        HashMap map = new HashMap();
        try {
            assertEquals(0, (int) floatExp.calcCoeffs(map));
        } catch (NonLinearExpression e) {
            fail("FloatExpAddExp treated as NonLinearExpression");
        }
        assertEquals(2, map.size());
        assertEquals(1, (int) ((Double) (map.get(floatVar1))).doubleValue());
        assertEquals(1, (int) ((Double) (map.get(floatVar2))).doubleValue());
    }

    public void testSetMinSetMax() {
        FloatVar floatVar1 = C.addFloatVar(0, 10, ""), floatVar2 = C.addFloatVar(0, 10, "");
        FloatExp addExp = new FloatExpAddExp(floatVar1, floatVar2);

        // descendant event propagation : setMax()
        try {
            assertEquals(20, addExp.max(), Constrainer.precision());
            addExp.setMax(9);
            C.propagate();
            assertEquals(9, floatVar1.max(), Constrainer.precision());
            assertEquals(9, floatVar2.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed due to incorrect work of FloatVar.setMin(double)");
        }

        // constraint propagation
        floatVar1 = C.addFloatVar(0, 10, "");
        floatVar2 = C.addFloatVar(0, 10, "");
        addExp = new FloatExpAddExp(floatVar1, floatVar2);
        try {
            C.postConstraint(addExp.le(15));
            floatVar1.setMin(8);
            floatVar2.setMin(8);
            C.propagate();
            fail("test failed: Constraint propagation");
        } catch (Failure f) {/* that's ok */
        }

        floatVar1 = C.addFloatVar(0, 10, "");
        floatVar2 = C.addFloatVar(0, 10, "");
        addExp = new FloatExpAddExp(floatVar1, floatVar2);
        try {
            C.postConstraint(addExp.le(16));
            floatVar1.setMin(8 + Constrainer.precision());
            C.propagate();
            assertEquals(8, floatVar2.max(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed: Constraint propagation");
        }
        // descendant event propagation : setMin()
        floatVar1 = C.addFloatVar(0, 10, "");
        floatVar2 = C.addFloatVar(0, 10, "");
        addExp = new FloatExpAddExp(floatVar1, floatVar2);
        try {
            addExp.setMin(12);
            C.propagate();
            assertEquals(2, floatVar1.min(), Constrainer.precision());
            assertEquals(2, floatVar2.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of setMin(double)");
        }
    }

}