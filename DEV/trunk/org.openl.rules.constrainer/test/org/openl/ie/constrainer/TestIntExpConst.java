package org.openl.ie.constrainer;

import java.util.HashMap;
import java.util.Map;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpConst;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpConst;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.NonLinearExpression;

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

public class TestIntExpConst extends TestCase {
    static private Constrainer C = new Constrainer("TestIntExpConst");
    static private IntExpConst positiveConst = new IntExpConst(C, 10);
    static private IntExpConst negativeConst = new IntExpConst(C, -10);
    static private IntExpConst zeroConst = new IntExpConst(C, 0);

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpConst.class));
    }

    public TestIntExpConst(String name) {
        super(name);
    }

    public void testAdd() {
        // add float
        FloatExp floatExp = positiveConst.add(5.2);
        assertEquals("add(float) works wrong", 5.2 + 10, floatExp.min(), Constrainer.FLOAT_MIN);
        assertEquals("add(float) works wrong", 5.2 + 10, floatExp.max(), Constrainer.FLOAT_MIN);
        assertTrue("add(float) works wrong", floatExp instanceof FloatExpConst);
        assertTrue(floatExp.isLinear());
        Map coeffs = new HashMap();
        try {
            assertEquals(5.2 + 10, floatExp.calcCoeffs(coeffs), Constrainer.FLOAT_MIN);
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());

        // add FloatExp
        FloatVar testFloatVar = C.addFloatVar(-5.2, 5.2, "testFloatVar");
        floatExp = positiveConst.add(testFloatVar);
        assertEquals("add(FloatExp) works wrong", -5.2 + 10, floatExp.min(), Constrainer.FLOAT_MIN);
        assertEquals("add(FloatExp) works wrong", 5.2 + 10, floatExp.max(), Constrainer.FLOAT_MIN);
        coeffs = new HashMap();
        assertTrue(floatExp.isLinear());
        try {
            assertEquals(10, floatExp.calcCoeffs(coeffs), Constrainer.FLOAT_MIN);
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertEquals(1, coeffs.size());
        FloatVar[] arr = new FloatVar[1];
        coeffs.keySet().toArray(arr);
        assertTrue(((Object) testFloatVar).equals(arr[0]));

        // add int
        IntExp intExp = positiveConst.add(5);
        assertEquals("add(int) works wrong", 5 + 10, intExp.min());
        assertEquals("add(int) works wrong", 5 + 10, intExp.max());
        assertTrue("add(int) works wrong", intExp instanceof IntExpConst);
        assertTrue("add(int) works wrong: the expression isn't Linear", intExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("add(int) works wrong: calcCoeffs problems", 5 + 10, (int) intExp.calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());

        // add IntVar
        IntVar testIntVar = C.addIntVar(-5, 5, "intVar");
        intExp = positiveConst.add(testIntVar);
        assertEquals("add(IntExp) works wrong", -5 + 10, intExp.min());
        assertEquals("add(IntExp) works wrong", 5 + 10, intExp.max());
        assertTrue("add(int) works wrong: the expression isn't Linear", intExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("add(int) works wrong: calcCoeffs problems", 10, (int) intExp.calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertEquals(1, coeffs.size());
        IntVar[] intvararr = new IntVar[1];
        coeffs.keySet().toArray(intvararr);
        assertTrue(((Object) intvararr[0]).equals(testIntVar));
        assertEquals(1, (int) ((Double) coeffs.get(testIntVar)).doubleValue());
        // event propagation test
        // descending
        try {
            int newmax = 10;
            intExp.setMax(newmax);
            C.propagate();
            assertEquals(newmax - 10, testIntVar.max());
            assertEquals(-5, testIntVar.min());

            int newmin = 6;
            intExp.setMin(newmin);
            C.propagate();
            assertEquals(newmax - 10, testIntVar.max());
            assertEquals(newmin - 10, testIntVar.min());
        } catch (Failure f) {
            fail("test failed due to incorrect work of ");
        }

        // ascending
        // testIntVar[-4..0]
        try {
            int newmax = -1;
            testIntVar.setMax(newmax);
            C.propagate();
            assertEquals(newmax + 10, intExp.max());
            assertEquals(6, intExp.min());

            int newmin = -3;
            testIntVar.setMin(newmin);
            C.propagate();
            assertEquals(newmax + 10, intExp.max());
            assertEquals(-3 + 10, intExp.min());
        } catch (Failure f) {
            fail("test failed due to incorrect work of ");
        }
    }

    public void testDiv() {
        IntExp intExp = positiveConst.div(10);
        assertEquals("mul(float) works wrong", 1, intExp.min());
        assertEquals("mul(float) works wrong", 1, intExp.max());
        assertTrue("mul(float) works wrong", intExp instanceof IntExpConst);
        assertTrue("mul(float) works wrong: isLinear() failed", intExp.isLinear());
        Map coeffs = new HashMap();
        try {
            assertEquals(1, intExp.calcCoeffs(coeffs), Constrainer.FLOAT_MIN);
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());

        // event propagation test
    }

    public void testEquals() {
        IntVar intvar = C.addIntVar(0, 9);
        try {
            C.postConstraint(positiveConst.equals(intvar));
            fail("test failed!");
        } catch (Failure f) {
        }
        intvar = C.addIntVar(5, 15);
        try {
            C.postConstraint(positiveConst.equals(intvar));
            assertEquals(10, intvar.value());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testMore() {
        IntVar intvar = C.addIntVar(10, 12);
        try {
            C.postConstraint(positiveConst.more(intvar));
            fail("test failed!");
        } catch (Failure f) {
        }
        intvar = C.addIntVar(5, 15);
        try {
            C.postConstraint(positiveConst.more(intvar));
            assertEquals(9, intvar.max());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testMoreOrEqual() {
        IntVar intvar = C.addIntVar(11, 12);
        try {
            C.postConstraint(positiveConst.moreOrEqual(intvar));
            fail("test failed!");
        } catch (Failure f) {
        }

        intvar = C.addIntVar(10, 15);
        try {
            C.postConstraint(positiveConst.moreOrEqual(intvar));
            assertEquals(10, intvar.value());
        } catch (Failure f) {
            fail("test failed!");
        }

        intvar = C.addIntVar(7, 15);
        try {
            C.postConstraint(positiveConst.moreOrEqual(intvar));
            assertEquals(10, intvar.max());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testMoreValue() {
        try {
            C.postConstraint(positiveConst.more(11));
            fail("test failed");
        } catch (Failure f) {
        }

        try {
            C.postConstraint(positiveConst.more(9));
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testMul() {
        // mul float
        FloatExp floatExp = positiveConst.mul(5.2);
        assertEquals("mul(float) works wrong", 5.2 * 10, floatExp.min(), Constrainer.FLOAT_MIN);
        assertEquals("mul(float) works wrong", 5.2 * 10, floatExp.max(), Constrainer.FLOAT_MIN);
        assertTrue("mul(float) works wrong", floatExp instanceof FloatExpConst);
        assertTrue("mul(float) works wrong: isLinear() failed", floatExp.isLinear());
        Map coeffs = new HashMap();
        try {
            assertEquals(5.2 * 10, floatExp.calcCoeffs(coeffs), Constrainer.FLOAT_MIN);
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());

        // mul FloatExp
        FloatVar testFloatVar = C.addFloatVar(-5.2, 5.2, "testFloatVar");
        floatExp = positiveConst.mul(testFloatVar);
        assertEquals("mul(FloatExp) works wrong", -5.2 * 10, floatExp.min(), Constrainer.FLOAT_MIN);
        assertEquals("mul(FloatExp) works wrong", 5.2 * 10, floatExp.max(), Constrainer.FLOAT_MIN);
        assertTrue("mul(float) works wrong: isLinear() failed", floatExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals(0, floatExp.calcCoeffs(coeffs), Constrainer.FLOAT_MIN);
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertEquals(1, coeffs.size());
        FloatVar[] arr = new FloatVar[1];
        coeffs.keySet().toArray(arr);
        assertTrue(((Object) testFloatVar).equals(arr[0]));
        assertEquals(10, ((Double) (coeffs.get(testFloatVar))).doubleValue(), Constrainer.FLOAT_MIN);
        // event propagation
        // descending
        try {
            double newmax = (5.2 * 10 - 3.4);
            floatExp.setMax(newmax);
            C.propagate();
            assertEquals(newmax / 10, testFloatVar.max(), Constrainer.FLOAT_MIN);

            double newmin = -5.2 * 10 + 3.4;
            floatExp.setMin(newmin);
            C.propagate();
            assertEquals(newmin / 10, testFloatVar.min(), Constrainer.FLOAT_MIN);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatExp.setMax(double)");
        }
        // ascending
        try {
            double newmax = (4.1);
            testFloatVar.setMax(newmax);
            C.propagate();
            assertEquals(newmax * 10, floatExp.max(), Constrainer.FLOAT_MIN);

            double newmin = -4.1;
            testFloatVar.setMin(newmin);
            C.propagate();
            assertEquals(newmin * 10, floatExp.min(), Constrainer.FLOAT_MIN);
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of FloatExp.setMax(double)");
        }

        // FloatExp mul IntExpConst
        testFloatVar = C.addFloatVar(-5.2, 5.2, "testFloatVar1");
        floatExp = testFloatVar.mul(positiveConst);
        assertEquals("FloatExp mul IntExpConst works wrong", -5.2 * 10, floatExp.min(), Constrainer.FLOAT_MIN);
        assertEquals("FloatExp mul IntExpConst works wrong", 5.2 * 10, floatExp.max(), Constrainer.FLOAT_MIN);
        assertTrue("FloatExp mul IntExpConst works wrong: isLinear() failed", floatExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals(0, floatExp.calcCoeffs(coeffs), Constrainer.FLOAT_MIN);
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertEquals(1, coeffs.size());
        arr = new FloatVar[1];
        coeffs.keySet().toArray(arr);
        assertTrue(((Object) testFloatVar).equals(arr[0]));
        assertEquals(10, ((Double) (coeffs.get(testFloatVar))).doubleValue(), Constrainer.FLOAT_MIN);

        // mul int
        IntExp intExp = positiveConst.mul(5);
        assertEquals("mul(int) works wrong", 5 * 10, intExp.min());
        assertEquals("mul(int) works wrong", 5 * 10, intExp.max());
        assertTrue("mul(int) works wrong", intExp instanceof IntExpConst);
        assertTrue("mul(int) works wrong: the expression isn't Linear", intExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("mul(int) works wrong: calcCoeffs problems", 5 * 10, (int) intExp.calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());

        // mul IntVar
        IntVar testIntVar = C.addIntVar(-5, 5, "intVar");
        intExp = positiveConst.mul(testIntVar);
        assertEquals("mul(IntExp) works wrong", -5 * 10, intExp.min());
        assertEquals("mul(IntExp) works wrong", 5 * 10, intExp.max());
        assertTrue("mul(IntExp) works wrong: the expression isn't Linear", intExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("mul(IntExp) works wrong: calcCoeffs problems", 0, (int) intExp.calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertEquals(1, coeffs.size());
        IntVar[] intvararr = new IntVar[1];
        coeffs.keySet().toArray(intvararr);
        assertTrue(((Object) intvararr[0]).equals(testIntVar));
        assertEquals(10, (int) ((Double) coeffs.get(testIntVar)).doubleValue());

        // IntVar mul IntExpConst
        testIntVar = C.addIntVar(-5, 5, "intVar");
        intExp = testIntVar.mul(positiveConst);
        assertEquals("IntVar mul IntExpConst works wrong", -5 * 10, intExp.min());
        assertEquals("IntVar mul IntExpConst works wrong", 5 * 10, intExp.max());
        assertTrue("IntVar mul IntExpConst works wrong: the expression isn't Linear", intExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("IntVar mul IntExpConst works wrong: calcCoeffs problems", 0, (int) intExp.calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertEquals(1, coeffs.size());
        intvararr = new IntVar[1];
        coeffs.keySet().toArray(intvararr);
        assertTrue(((Object) intvararr[0]).equals(testIntVar));
        assertEquals(10, (int) ((Double) coeffs.get(testIntVar)).doubleValue());

        // IntExpConst mul IntExpConst
        intExp = positiveConst.mul(negativeConst);
        assertEquals("IntExpConst mul IntExpConst works wrong", 10 * (-10), intExp.min());
        assertEquals("IntExpConst mul IntExpConst works wrong", 10 * (-10), intExp.max());
        assertTrue("IntExpConst mul IntExpConst works wrong", intExp instanceof IntExpConst);
        assertTrue("IntExpConst mul IntExpConst works wrong: the expression isn't Linear", intExp.isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("IntExpConst mul IntExpConst works wrong: calcCoeffs problems", 10 * (-10), (int) intExp
                    .calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());

        // Zero valued IntExpConst mul IntExpConst
        testIntVar = C.addIntVar(-5, 5, "intVar");
        intExp = zeroConst.mul(testIntVar);
        assertEquals("Zero valued IntExpConst mul IntExpConst works wrong", 0, intExp.min());
        assertEquals("Zero valued IntExpConst mul IntExpConst works wrong", 0, intExp.max());
        assertTrue("Zero valued IntExpConst mul IntExpConst works wrong", intExp instanceof IntExpConst);
        assertTrue("Zero valued IntExpConst mul IntExpConst works wrong: the expression isn't Linear", intExp
                .isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("Zero valued IntExpConst mul IntExpConst works wrong: calcCoeffs problems", 0, (int) intExp
                    .calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());

        // IntExp mul zero valued IntExpConst
        testIntVar = C.addIntVar(-5, 5, "intVar");
        intExp = testIntVar.mul(zeroConst);
        assertEquals("Zero valued IntExpConst mul IntExpConst works wrong", 0, intExp.min());
        assertEquals("Zero valued IntExpConst mul IntExpConst works wrong", 0, intExp.max());
        assertTrue("Zero valued IntExpConst mul IntExpConst works wrong", intExp instanceof IntExpConst);
        assertTrue("Zero valued IntExpConst mul IntExpConst works wrong: the expression isn't Linear", intExp
                .isLinear());
        coeffs = new HashMap();
        try {
            assertEquals("Zero valued IntExpConst mul IntExpConst works wrong: calcCoeffs problems", 0, (int) intExp
                    .calcCoeffs(coeffs));
        } catch (NonLinearExpression ex) {/* it will never happen */
        }
        assertTrue(coeffs.isEmpty());
        // assertEquals(0, (int)((Double)coeffs.get(testIntVar)).doubleValue());
    }

    public void testNeg() {
        IntExp exp = negativeConst.neg();
        assertTrue(exp instanceof IntExpConst);
        assertEquals(10, exp.max());
        assertEquals(10, exp.min());
    }

}