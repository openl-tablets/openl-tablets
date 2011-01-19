package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpConst;
import org.openl.ie.constrainer.IntExpConst;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.impl.FloatExpAbs;
import org.openl.ie.constrainer.impl.FloatExpAddExp;
import org.openl.ie.constrainer.impl.FloatExpAddValue;
import org.openl.ie.constrainer.impl.FloatExpExponent;
import org.openl.ie.constrainer.impl.FloatExpInverse;
import org.openl.ie.constrainer.impl.FloatExpLog;
import org.openl.ie.constrainer.impl.FloatExpMulExp;
import org.openl.ie.constrainer.impl.FloatExpMultiplyPositive;
import org.openl.ie.constrainer.impl.FloatExpOpposite;
import org.openl.ie.constrainer.impl.FloatExpSqr;
import org.openl.ie.constrainer.impl.FloatVarImpl;
import org.openl.ie.constrainer.impl.IntVarImpl;

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

public class TestFloatVarImpl extends TestCase {
    private Constrainer C = new Constrainer("TestFloatVarImpl");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestFloatVarImpl.class));
    }

    public TestFloatVarImpl(String name) {
        super(name);
    }

    public void testAbs() {
        FloatVarImpl floatVar = new FloatVarImpl(C, -2, 5, "");
        FloatExp floatExp = floatVar.abs();
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.abs() is to be retested", floatExp instanceof FloatExpAbs);

        FloatExpConst floatConst = new FloatExpConst(C, -2);
        floatExp = floatConst.abs();
        assertTrue("FloatExpImpl.abs() is to be retested", floatExp instanceof FloatExpConst);
        assertEquals(2, floatExp.max(), Constrainer.precision());
    }

    public void testAdd() {
        FloatVarImpl floatVar1 = new FloatVarImpl(C, -2, 5, ""), floatVar2 = new FloatVarImpl(C, -5, 3, "");

        FloatExp floatExp = floatVar1.add(floatVar2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(FloatExp) is to be retested", floatExp instanceof FloatExpAddExp);

        IntVar intVar1 = new IntVarImpl(C, -2, 7, "");
        floatExp = floatVar1.add(intVar1);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(IntExp) is to be retested", floatExp instanceof FloatExpAddExp);

        floatExp = floatVar1.add(2.342);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(double) is to be retested", floatExp instanceof FloatExpAddValue);

        floatExp = floatVar1.add(2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(int) is to be retested", floatExp instanceof FloatExpAddValue);

        FloatExpConst floatConst = new FloatExpConst(C, 9);
        floatExp = floatVar1.add(floatConst);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(FloatExpConst) is to be retested", floatExp instanceof FloatExpAddValue);

        IntExpConst intConst = new IntExpConst(C, 9);
        floatExp = floatVar1.add(floatConst);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(IntExpConst) is to be retested", floatExp instanceof FloatExpAddValue);

        floatExp = floatConst.add(floatVar2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(FloatExp) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpAddValue);

        floatExp = floatConst.add(intVar1);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.add(IntExp) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpAddValue);

        floatExp = floatConst.add(3.0);
        assertTrue("FloatExpImpl.add(double) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpConst);
        assertEquals(9 + 3, floatExp.max(), Constrainer.precision());
    }

    public void testDiv() {
        FloatVarImpl floatVar1 = new FloatVarImpl(C, -2, 5, ""), floatVar2 = new FloatVarImpl(C, -5, 3, "");

        FloatExp floatExp = floatVar1.div(floatVar2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(FloatExp) is to be retested", floatExp instanceof FloatExpMulExp);

        IntVar intVar1 = new IntVarImpl(C, -2, 7, "");
        floatExp = floatVar1.div(intVar1);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(IntExp) is to be retested", floatExp instanceof FloatExpMulExp);

        floatExp = floatVar1.div(2.342);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(double) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatVar1.div(2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(int) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        FloatExpConst floatConst = new FloatExpConst(C, 9);
        floatExp = floatVar1.div(floatConst);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(FloatExpConst) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        IntExpConst intConst = new IntExpConst(C, 9);
        floatExp = floatVar1.div(floatConst);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(IntExpConst) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatConst.div(floatVar2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(FloatExp) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatConst.div(intVar1);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.div(IntExp) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatConst.div(3.0);
        assertTrue("FloatExpImpl.div(double) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpConst);
        assertEquals(9 / 3, floatExp.max(), Constrainer.precision());
    }

    public void testExp() {
        FloatVarImpl floatVar = new FloatVarImpl(C, -2, 5, "");
        FloatExp floatExp = null;
        floatExp = floatVar.exp();
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.exp() is to be retested", floatExp instanceof FloatExpExponent);

        FloatExpConst floatConst = new FloatExpConst(C, 1);
        floatExp = floatConst.exp();
        assertTrue("FloatExpImpl.exp() is to be retested", floatExp instanceof FloatExpConst);
        assertEquals(Math.E, floatExp.max(), Constrainer.precision());
    }

    public void testInv() {
        FloatVarImpl floatVar = new FloatVarImpl(C, -2, 5, "");
        FloatExp floatExp = floatVar.inv();
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.inv() is to be retested", floatExp instanceof FloatExpInverse);

        FloatExpConst floatConst = new FloatExpConst(C, -2);
        floatExp = floatConst.inv();
        assertTrue("FloatExpImpl.inv() is to be retested", floatExp instanceof FloatExpConst);
        assertEquals(-1.0 / 2.0, floatExp.max(), Constrainer.precision());
    }

    public void testLog() {
        FloatVarImpl floatVar = new FloatVarImpl(C, -2, 5, "");
        FloatExp floatExp = null;
        try {
            floatExp = floatVar.log();
        } catch (Failure f) {
            fail("test failed");
        }
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.log() is to be retested", floatExp instanceof FloatExpLog);

        FloatExpConst floatConst = new FloatExpConst(C, 2);
        try {
            floatExp = floatConst.log();
        } catch (Failure f) {
            fail("test failed");
        }
        assertTrue("FloatExpImpl.log() is to be retested", floatExp instanceof FloatExpConst);
        assertEquals(Math.log(2), floatExp.max(), Constrainer.precision());
    }

    public void testMul() {
        FloatVarImpl floatVar1 = new FloatVarImpl(C, -2, 5, ""), floatVar2 = new FloatVarImpl(C, -5, 3, "");

        FloatExp floatExp = floatVar1.mul(floatVar2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(FloatExp) is to be retested", floatExp instanceof FloatExpMulExp);

        IntVar intVar1 = new IntVarImpl(C, -2, 7, "");
        floatExp = floatVar1.mul(intVar1);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(IntExp) is to be retested", floatExp instanceof FloatExpMulExp);

        floatExp = floatVar1.mul(2.342);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(double) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatVar1.mul(2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(int) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        FloatExpConst floatConst = new FloatExpConst(C, 9);
        floatExp = floatVar1.mul(floatConst);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(FloatExpConst) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        IntExpConst intConst = new IntExpConst(C, 9);
        floatExp = floatVar1.mul(floatConst);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(IntExpConst) is to be retested", floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatConst.mul(floatVar2);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(FloatExp) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatConst.mul(intVar1);
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.mul(IntExp) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpMultiplyPositive);

        floatExp = floatConst.mul(3.0);
        assertTrue("FloatExpImpl.mul(double) is to be retested: FloatExpImpl is bound",
                floatExp instanceof FloatExpConst);
        assertEquals(9 * 3, floatExp.max(), Constrainer.precision());
    }

    public void testNeg() {
        FloatVarImpl floatVar = new FloatVarImpl(C, -2, 5, "");
        FloatExp floatExp = floatVar.neg();
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.neg() is to be retested", floatExp instanceof FloatExpOpposite);

        FloatExpConst floatConst = new FloatExpConst(C, -2);
        floatExp = floatConst.neg();
        assertTrue("FloatExpImpl.neg() is to be retested", floatExp instanceof FloatExpConst);
        assertEquals(2, floatExp.max(), Constrainer.precision());
    }

    public void testSetMaxSetMinAndCreateUndo() {
        FloatVarImpl floatVar = new FloatVarImpl(C, -2, 5, "");
        Undo undo = floatVar.createUndo();
        undo.undoable(floatVar);
        try {
            floatVar.setMax(3.123);
            floatVar.setMin(-1.123);
            C.propagate();
            assertEquals(3.123, floatVar.max(), Constrainer.precision());
            assertEquals(-1.123, floatVar.min(), Constrainer.precision());
        } catch (Failure f) {
            fail("test failed");
        }
        undo.undo();
        assertEquals(5, floatVar.max(), Constrainer.precision());
        assertEquals(-2, floatVar.min(), Constrainer.precision());
    }

    public void testSqr() {
        FloatVarImpl floatVar = new FloatVarImpl(C, -2, 5, "");
        FloatExp floatExp = null;
        floatExp = floatVar.sqr();
        assertTrue(floatExp != null);
        assertTrue("FloatExpImpl.sqr() is to be retested", floatExp instanceof FloatExpSqr);

        FloatExpConst floatConst = new FloatExpConst(C, 3);
        floatExp = floatConst.sqr();
        assertTrue("FloatExpImpl.sqr() is to be retested", floatExp instanceof FloatExpConst);
        assertEquals(9, floatExp.max(), Constrainer.precision());
    }

}