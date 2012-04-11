package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntExpDivExp;
import org.openl.ie.constrainer.impl.IntExpDivExp2;

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

public class TestIntExpDivExp extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpAbs");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpDivExp.class));
    }

    public TestIntExpDivExp(String name) {
        super(name);
    }

    private void curTestDiv(IntExp numerator, IntExp denominator, IntExp quotient, IntBoolExp quotientConstraint,
            IntBoolExp numeratorConstraint, IntBoolExp denominatorConstraint) {

        try {
            C.postConstraint(quotientConstraint);
        } catch (Failure f) {
            fail("quotientConstraint posting failed");
        }
        try {
            C.postConstraint(numeratorConstraint);
        } catch (Failure f) {
            fail("numeratorConstraint posting failed");
        }
        try {
            C.postConstraint(denominatorConstraint);
        } catch (Failure f) {
            fail("denominatorConstraint posting failed");
        }
        boolean flag = C.execute(new GoalGenerate(new IntExpArray(C, numerator, denominator)));
        assertTrue("there were no solutions", flag);
        try {
            assertEquals("IntExpDivExp works wrong!", quotient.value(), numerator.value() / denominator.value());
            System.out.println("numerator: " + numerator.value());
            System.out.println("denominator: " + denominator.value());
            System.out.println("quotient: " + quotient.value());
            assertTrue(quotientConstraint.value() == 1);
            assertTrue(numeratorConstraint.value() == 1);
            assertTrue(denominatorConstraint.value() == 1);
        } catch (Failure f) {
            fail("test suddenly failed while attempting to get value of bound variable");
        }
    }

    public void testCase1() {
        // case 1 : numerator > 0 , denominator > 0
        IntVar numerator = C.addIntVar(1, 10), denominator = C.addIntVar(1, 8);
        IntExp quotient = null;
        System.out.println("case 1 : numerator[1..10] > 0 , denominator[1..8] > 0");
        System.out.println("The problem was: quotient = 2, numerator > 4, denominator < 4: ");
        quotient = new IntExpDivExp(numerator, denominator);
        curTestDiv(numerator, denominator, quotient, quotient.eq(2), numerator.gt(4), denominator.lt(4));

        // another implementation
        System.out.println("another implementation");
        numerator = C.addIntVar(1, 10);
        denominator = C.addIntVar(1, 8);
        try {
            quotient = new IntExpDivExp2(numerator, denominator);
        } catch (Failure f) {
            fail("test failed");
        }
        curTestDiv(numerator, denominator, quotient, quotient.eq(2), numerator.gt(4), denominator.lt(4));
    }

    public void testCase2() {
        IntVar numerator = C.addIntVar(-9, 12), denominator = C.addIntVar(1, 8);
        IntExp quotient = null;
        System.out.println("case 2 : numerator[-4..12] - variable sign , denominator[1..8] > 0");
        System.out.println("The problem was: quotient = 2, numerator < -2, denominator < 6");
        quotient = new IntExpDivExp(numerator, denominator);
        curTestDiv(numerator, denominator, quotient, quotient.eq(-2), numerator.lt(-2), denominator.lt(6));
        // another implementation
        System.out.println("another implementation");
        numerator = C.addIntVar(-9, 12);
        denominator = C.addIntVar(1, 8);
        try {
            quotient = new IntExpDivExp2(numerator, denominator);
        } catch (Failure f) {
            fail("test failed");
        }
        curTestDiv(numerator, denominator, quotient, quotient.eq(-2), numerator.lt(-2), denominator.lt(6));
    }

    public void testCase3() {
        IntVar numerator = C.addIntVar(-9, -1), denominator = C.addIntVar(1, 8);
        IntExp quotient = null;
        System.out.println("case 3 : numerator[" + numerator.min() + ".." + numerator.max() + "] < 0 ,"
                + " denominator[" + denominator.min() + ".." + denominator.max() + "] > 0");
        System.out.println("The problem was: quotient = 2, numerator < -2, denominator < 6");
        try {
            quotient = new IntExpDivExp(numerator, denominator);
            // to add the principal possibility of throwing failures
            numerator.setMax(10000);
        } catch (Failure f) {
            fail("test failed");
        }
        curTestDiv(numerator, denominator, quotient, quotient.eq(-2), numerator.lt(-2), denominator.lt(6));
    }

    public void testCase4() {
        IntVar numerator = C.addIntVar(1, 22), denominator = C.addIntVar(-7, -3);
        IntExp quotient = null;
        System.out.println("case 4 : numerator[" + numerator.min() + ".." + numerator.max() + "] > 0 ,"
                + " denominator[" + denominator.min() + ".." + denominator.max() + "] < 0");
        System.out.println("The problem was: quotient = -2, numerator > 7, denominator < -4");
        try {
            quotient = new IntExpDivExp(numerator, denominator);
            // to add the principal possibility of throwing failures
            numerator.setMax(10000);
        } catch (Failure f) {
            fail("test failed");
        }
        curTestDiv(numerator, denominator, quotient, quotient.eq(-2), numerator.gt(7), denominator.lt(-4));
    }

    public void testCase5() {
        IntVar numerator = C.addIntVar(1, 22), denominator = C.addIntVar(-7, 11);
        IntExp quotient = null;
        System.out.println("case 5 : numerator[" + numerator.min() + ".." + numerator.max() + "] > 0 ,"
                + " denominator[" + denominator.min() + ".." + denominator.max() + "] of variable sign");
        System.out.println("The problem was: quotient = 5, numerator > 7, denominator > -5");
        quotient = new IntExpDivExp(numerator, denominator);
        curTestDiv(numerator, denominator, quotient, quotient.eq(5), numerator.gt(7), denominator.gt(-5));

        // another implementation
        System.out.println("another implementation");
        numerator = C.addIntVar(1, 22);
        denominator = C.addIntVar(-7, 11);
        try {
            quotient = new IntExpDivExp2(numerator, denominator);
        } catch (Failure f) {
            fail("test failed");
        }
        curTestDiv(numerator, denominator, quotient, quotient.eq(5), numerator.gt(7), denominator.gt(-5));
    }

    public void testCase6() {
        IntVar numerator = C.addIntVar(-32, -4), denominator = C.addIntVar(-56, -14);
        IntExp quotient = null;
        System.out.println("case 6 : numerator[" + numerator.min() + ".." + numerator.max() + "] < 0 ,"
                + " denominator[" + denominator.min() + ".." + denominator.max() + "] < 0");
        System.out.println("The problem was: quotient = 5, numerator < -7, denominator > -23");
        try {
            quotient = new IntExpDivExp(numerator, denominator);
            // to add the principal possibility of throwing failures
            numerator.setMax(10000);
        } catch (Failure f) {
            fail("test failed");
        }
        curTestDiv(numerator, denominator, quotient, quotient.eq(2), numerator.lt(-7), denominator.gt(-23));

    }

    public void testCase7() {
        IntVar numerator = C.addIntVar(-32, 43), denominator = C.addIntVar(-32, 43);
        IntExp quotient = null;
        System.out.println("case 7 : numerator[" + numerator.min() + ".." + numerator.max() + "] of variable sign ,"
                + " denominator[" + denominator.min() + ".." + denominator.max() + "] of variable sign");
        System.out.println("The problem was: quotient = 5, numerator > -29, denominator > -3");
        try {
            quotient = new IntExpDivExp(numerator, denominator);
            // to add the principal possibility of throwing failures
            numerator.setMax(10000);
        } catch (Failure f) {
            fail("test failed");
        }
        curTestDiv(numerator, denominator, quotient, quotient.eq(5), numerator.gt(-29), denominator.gt(-3));

    }

}