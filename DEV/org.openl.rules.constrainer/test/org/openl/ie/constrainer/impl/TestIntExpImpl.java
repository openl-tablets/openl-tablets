package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpConst;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.FloatExpAddExp;
import org.openl.ie.constrainer.impl.FloatExpAddValue;
import org.openl.ie.constrainer.impl.FloatExpMulExp;
import org.openl.ie.constrainer.impl.FloatExpMultiplyPositive;
import org.openl.ie.constrainer.impl.IntExpAbs;
import org.openl.ie.constrainer.impl.IntExpAddExp;
import org.openl.ie.constrainer.impl.IntExpAddValue;
import org.openl.ie.constrainer.impl.IntExpMulExp;
import org.openl.ie.constrainer.impl.IntExpMultiplyPositive;
import org.openl.ie.constrainer.impl.IntExpOpposite;

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

public class TestIntExpImpl extends TestCase {
    static private Constrainer C = new Constrainer("TestIntExpImpl");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpImpl.class));
    }

    public TestIntExpImpl(String name) {
        super(name);
    }

    public void testAbs() {
        IntExp pilotExp = C.addIntVar(-10, 10, "intexp");
        IntExp absexp = pilotExp.abs();
        if (!(absexp instanceof IntExpAbs)) {
            fail("test failed");
        }
    }

    public void testAdd() {
        IntExp pilotExp = C.addIntVar(-10, 10, "intexp");
        IntExp addexp = pilotExp.add(C.addIntVar(0, 3));
        if (!(addexp instanceof IntExpAddExp)) {
            fail("IntExpImpl.add(IntExp): test failed");
        }
        addexp = pilotExp.add(5);
        if (!(addexp instanceof IntExpAddValue)) {
            fail("IntExpImpl.add(int): test failed");
        }
        FloatExp floatexp = pilotExp.add(4.2);
        if (!(floatexp instanceof FloatExpAddValue)) {
            fail("IntExpImpl.add(double): test failed");
        }

        try {
            floatexp = pilotExp.add(C.addFloatVar(0, 1));
        } catch (Failure f) {
            fail("IntExpImpl.add(double): test failed");
        }
        if (!(floatexp instanceof FloatExpAddExp)) {
            fail("IntExpImpl.add(double): test failed");
        }
    }

    public void testDiv() {
        IntVar intvar = C.addIntVar(-4, 11);
        FloatExp floatExp = intvar.div(3.21);
        if (!(floatExp instanceof FloatExpMultiplyPositive)) {
            fail("IntExp.div(double) : test failed");
        }

        try {
            IntExp intexp = intvar.div(0);
            fail("allows division by zero!");
        } catch (IllegalArgumentException ex) {/* that's ok */
        }

        IntExp intexp = intvar.div(1);
        if (!((Object) intexp).equals(intvar)) {
            fail("division by unity : test failed");
        }

        intexp = intvar.div(-1);
        if (!(intexp instanceof IntExpOpposite)) {
            fail("division by -1");
        }

        intexp = intvar.div(3);
        assertEquals((11 - 11 % 3) / 3, intexp.max());
        assertEquals((-4 + 4 % 3) / 3, intexp.min());
    }

    public void testMul() {
        IntExp pilotExp = C.addIntVar(-5, 10, "intexp");
        IntExp mulexp = pilotExp.mul(C.addIntVar(0, 3));
        if (!(mulexp instanceof IntExpMulExp)) {
            fail("IntExpImpl.mul(IntExp): test failed");
        }

        // multipliply by zero
        mulexp = pilotExp.mul(0);
        if (!(mulexp instanceof IntExpConst && (mulexp.min() == mulexp.max()) && (mulexp.min() == 0))) {
            fail("IntExpImpl.mul(0): test failed");
        }
        // multiply by zero IntExpConst
        mulexp = pilotExp.mul(new IntExpConst(C, 0));
        if (!(mulexp instanceof IntExpConst && (mulexp.min() == mulexp.max()) && (mulexp.min() == 0))) {
            fail("IntExpImpl.mul(IntExpConst(0)): test failed");
        }

        // multiply by -1
        mulexp = pilotExp.mul(-1);
        if (!(mulexp instanceof IntExpOpposite && (((Object) mulexp.neg()).equals(pilotExp)))) {
            fail("IntExpImpl.mul(-1): test failed");
        }

        // multiply by IntExpConst(-1)
        mulexp = pilotExp.mul(new IntExpConst(C, -1));
        if (!(mulexp instanceof IntExpOpposite && (((Object) mulexp.neg()).equals(pilotExp)))) {
            fail("IntExpImpl.mul(IntExpConst(-1)): test failed");
        }

        // multiply by IntExpConst(1)
        mulexp = pilotExp.mul(new IntExpConst(C, 1));
        if (!((Object) mulexp).equals(pilotExp)) {
            fail("IntExpImpl.mul(IntExpConst(1)): test failed");
        }

        // multiply by 1
        mulexp = pilotExp.mul(1);
        if (!((Object) mulexp).equals(pilotExp)) {
            fail("IntExpImpl.mul(1): test failed");
        }

        // multiply positive constant
        mulexp = pilotExp.mul(5);
        if (!(mulexp instanceof IntExpMultiplyPositive)) {
            // if the condition is true see TestIntExpMultiplyPositive class
            fail("IntExpImpl.mul(positiveInt): test failed");
        }
        // multiply negative constant
        mulexp = pilotExp.mul(-5);
        assertEquals(-50, mulexp.min());
        assertEquals(25, mulexp.max());
        try {
            // ascending event propagation
            pilotExp.setMin(-3);
            C.propagate();
            assertEquals(-3 * (-5), mulexp.max());
            assertEquals(10 * (-5), mulexp.min());

            pilotExp.setMax(7);
            C.propagate();
            assertEquals(-3 * (-5), mulexp.max());
            assertEquals(7 * (-5), mulexp.min());

            // descending event propagation
            mulexp.setMin(-31);
            assertEquals(-31 - (-31 % 5), mulexp.min());
            C.propagate();
            assertEquals(mulexp.min() / (-5), pilotExp.max());
            assertEquals(-3, pilotExp.min());

            mulexp.setMax(12);
            assertEquals(12 - (12 % 5), mulexp.max());
            C.propagate();
            assertEquals(mulexp.max() / (-5), pilotExp.min());
        } catch (Failure f) {
            fail("test failed due to incorrect behaviour of IntExpImpl.setMax()");
        }

        FloatExp floatexp = pilotExp.mul(-4.2);
        if (!(floatexp instanceof FloatExpMultiplyPositive)) {
            fail("IntExpImpl.mul(negative constant of type int): test failed");
        }

        try {
            floatexp = pilotExp.mul(C.addFloatVar(0, 1));
        } catch (Failure f) {
            fail("IntExpImpl.add(double): test failed");
        }
        if (!(floatexp instanceof FloatExpMulExp)) {
            fail("IntExpImpl.mul(negative constant of type int): test failed");
        }
    }

}