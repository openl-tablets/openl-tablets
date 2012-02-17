package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntExpPowIntExp;

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

public class TestIntExpPowIntExp extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpPowIntExp");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpPowIntExp.class));
    }

    public TestIntExpPowIntExp(String name) {
        super(name);
    }

    private void removeAllEven(IntVar var) {
        for (int i = var.min(); i <= var.max(); i++) {
            if (var.contains(i) && ((i % 2) == 0)) {
                try {
                    var.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            C.propagate();
        } catch (Failure f) {
            fail("Constrainer propagation failed");
        }
    }

    private void removeAllOdd(IntVar var) {
        for (int i = var.min(); i <= var.max(); i++) {
            if (var.contains(i) && ((i % 2) == 1)) {
                try {
                    var.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            C.propagate();
        } catch (Failure f) {
            fail("Constrainer propagation failed");
        }
    }

    public void testA(int testDoesntWork) {
        try {
            int resultValue = 9;
            IntVar base = C.addIntVar(-5, 5), exponent = C.addIntVar(0, 4);

            IntExpArray array = new IntExpArray(C, base, exponent);
            GoalGenerate golgen = new GoalGenerate(array);

            IntExp exp = new IntExpPowIntExp(base, exponent);
            // IntExp exp = new IntExpAddExp(intvar1, intvar2);
            C.postConstraint(exp.eq(resultValue));
            boolean flag = C.execute(golgen);
            assertTrue(flag);
            assertEquals(resultValue, Math.pow(base.value(), exponent.value()), 1);
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testMaxComplete() {
        // even powers && changable base
        IntVar base = C.addIntVar(-21, 3, "base"), power = C.addIntVar(2, 10, "power");
        IntExp exp = new IntExpPowIntExp(base, power);
        removeAllOdd(power);
        assertEquals("even powers && changable base", (int) Math.pow(Math.max(-base.min(), base.max()), power.max()),
                exp.max());
        // even powers && positive base
        base = C.addIntVar(-21, 3, "base");
        power = C.addIntVar(2, 10, "power");
        exp = new IntExpPowIntExp(base, power);
        removeAllOdd(power);
        try {
            base.setMin(1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("even powers && positive base", (int) Math.pow(Math.max(-base.min(), base.max()), power.max()),
                exp.max());

        // even powers && negative base
        base = C.addIntVar(-21, 3, "base");
        power = C.addIntVar(2, 10, "power");
        exp = new IntExpPowIntExp(base, power);
        removeAllOdd(power);
        try {
            base.setMax(-1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("even powers && negative base", (int) Math.pow(Math.max(-base.min(), base.max()), power.max()),
                exp.max());

        // odd powers && changable base
        base = C.addIntVar(-21, 12, "base");
        power = C.addIntVar(1, 11, "power");
        exp = new IntExpPowIntExp(base, power);
        assertEquals("any power && changable base...", (int) Math.pow(21, 10), exp.max());
        removeAllEven(power);
        // maximum has to decrease: was 21^10, now 12^11
        assertEquals("odd powers && changable base", (int) Math.pow(base.max(), power.max()), exp.max());
        assertEquals("odd powers && changable base", (int) Math.pow(12, 11), exp.max());

        // odd powers && negative base
        base = C.addIntVar(-21, 12, "base");
        power = C.addIntVar(1, 11, "power");
        exp = new IntExpPowIntExp(base, power);
        removeAllEven(power);
        try {
            base.setMax(-1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("odd powers && negative base", (int) Math.pow(base.max(), power.min()), exp.max());

        // odd powers && positive base
        base = C.addIntVar(-21, 12, "base");
        power = C.addIntVar(1, 11, "power");
        exp = new IntExpPowIntExp(base, power);
        removeAllEven(power);
        try {
            base.setMin(2);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("odd powers && positive base", (int) Math.pow(base.max(), power.max()), exp.max());

        // any powers && negative base
        base = C.addIntVar(-21, -12, "base");
        power = C.addIntVar(1, 11, "power");
        exp = new IntExpPowIntExp(base, power);
        assertEquals("any powers && negative base", (int) Math.pow(base.min(), 10 /*
                                                                                     * power's
                                                                                     * max
                                                                                     * even
                                                                                     */), exp.max());
        // any powers && positive base
        base = C.addIntVar(12, 21, "base");
        power = C.addIntVar(1, 11, "power");
        exp = new IntExpPowIntExp(base, power);
        assertEquals("any powers && positive base", (int) Math.pow(base.max(), power.max()), exp.max());

        // any powers && changable base
        base = C.addIntVar(-21, 5, "base");
        power = C.addIntVar(1, 11, "power");
        exp = new IntExpPowIntExp(base, power);
        assertEquals("any powers && positive base", (int) Math.max(Math.pow(base.max(), power.max()), Math.pow(base
                .min(), 10 /* power.max_even() */)), exp.max());
    }

    public void testMaxMinCase1() {
        // case 1: negative base
        IntVar base = C.addIntVar(-5, -1, "base", IntVar.DOMAIN_PLAIN), power = C.addIntVar(2, 6, "power",
                IntVar.DOMAIN_BIT_FAST);

        IntExp exp = new IntExpPowIntExp(base, power);
        assertEquals(-Math.pow(5, 5), exp.min(), Double.MIN_VALUE);
        assertEquals(Math.pow(5, 6), exp.max(), Double.MIN_VALUE);

        // remove all odd values from the power's domain
        for (int i = power.min(); i <= power.max(); i++) {
            if (power.contains(i) && ((i % 2) == 1)) {
                try {
                    power.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            C.propagate();
        } catch (Failure f) {
            fail("unexpected exception");
        }
        assertEquals(1, exp.min(), Double.MIN_VALUE);
        assertEquals(Math.pow(5, 6), exp.max(), Double.MIN_VALUE);

        power = C.addIntVar(0, 8);
        exp = new IntExpPowIntExp(base, power);
        assertEquals((int) -Math.pow(5, 7), exp.min());
        assertEquals((int) Math.pow(5, 8), exp.max());
        // remove all even values from the power's domain
        for (int i = power.min(); i <= power.max(); i++) {
            if (power.contains(i) && ((i % 2) == 0)) {
                try {
                    power.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            C.propagate();
        } catch (Failure f) {
            fail("unexpected exception");
        }
        assertEquals((int) -Math.pow(5, 7), exp.min());
        assertEquals(-1, exp.max());
    }

    public void testMaxMinCase2() {
        // case 2: positive base
        IntVar base = C.addIntVar(2, 7, "base", IntVar.DOMAIN_PLAIN), power = C.addIntVar(0, 8, "power",
                IntVar.DOMAIN_BIT_FAST);

        IntExp exp = new IntExpPowIntExp(base, power);
        assertEquals(1, exp.min());
        assertEquals((int) Math.pow(7, 8), exp.max());

        // remove all odd values from the power's domain
        for (int i = power.min(); i <= power.max(); i++) {
            if (power.contains(i) && ((i % 2) == 1)) {
                try {
                    power.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            C.propagate();
        } catch (Failure f) {
            fail("unexpected exception");
        }
        assertEquals(1, exp.min(), Double.MIN_VALUE);
        assertEquals((int) Math.pow(7, 8), exp.max());

        power = C.addIntVar(0, 8);
        exp = new IntExpPowIntExp(base, power);
        assertEquals(1, exp.min());
        assertEquals((int) Math.pow(7, 8), exp.max());
        // remove all even values from the power's domain
        for (int i = power.min(); i <= power.max(); i++) {
            if (power.contains(i) && ((i % 2) == 0)) {
                try {
                    power.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            C.propagate();
        } catch (Failure f) {
            fail("unexpected exception");
        }
        assertEquals(2, exp.min());
        assertEquals((int) Math.pow(7, 7), exp.max());
    }

    public void testMaxMinCase3() {
        // case 3: base changes sign
        IntVar base = C.addIntVar(-9, 7, "base", IntVar.DOMAIN_BIT_FAST), power = C.addIntVar(2, 8, "power",
                IntVar.DOMAIN_BIT_FAST);

        IntExp exp = new IntExpPowIntExp(base, power);
        assertEquals((int) Math.pow(-9, 7), exp.min());
        assertEquals((int) Math.pow(9, 8), exp.max());

        // remove all odd values from the power's domain
        for (int i = power.min(); i <= power.max(); i++) {
            if (power.contains(i) && ((i % 2) == 1)) {
                try {
                    power.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            base.removeValue(0);
            C.propagate();
        } catch (Failure f) {
            fail("unexpected exception");
        }
        assertEquals(1, exp.min());
        assertEquals((int) Math.pow(9, 8), exp.max());

        base = C.addIntVar(-5, 2);
        power = C.addIntVar(1, 5);
        exp = new IntExpPowIntExp(base, power);
        assertEquals((int) -Math.pow(5, 5), exp.min());
        assertEquals((int) Math.pow(5, 4), exp.max());
        // remove all even values from the power's domain
        for (int i = power.min(); i <= power.max(); i++) {
            if (power.contains(i) && ((i % 2) == 0)) {
                try {
                    power.removeValue(i);
                } catch (Failure f) {
                    fail("IntVar.removeValue works incorrectly");
                }
            }
        }
        try {
            C.propagate();
        } catch (Failure f) {
            fail("unexpected exception");
        }
        assertEquals((int) -Math.pow(5, 5), exp.min());
        // maximum has been decreased - it is important
        assertEquals((int) Math.pow(2, 5), exp.max());

        // base.max()^power.max() < base.min()^power.max_even()
        base = C.addIntVar(-5, 2);
        power = C.addIntVar(1, 5);
        exp = new IntExpPowIntExp(base, power);
        assertEquals((int) -Math.pow(5, 5), exp.min());
        assertEquals((int) Math.pow(5, 4), exp.max());
    }

    public void testMinComplete() {
        IntVar base = null, power = null;
        IntExp exp = null;

        // odd powers && positive base
        base = C.addIntVar(-5, 4);
        power = C.addIntVar(1, 5);
        exp = new IntExpPowIntExp(base, power);
        // only odd powers
        removeAllEven(power);
        // base becomes positive
        try {
            base.setMin(2);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("odd powers && positive base", (int) Math.pow(base.min(), power.min()), exp.min());

        // odd powers and negative base
        base = C.addIntVar(-5, 4);
        power = C.addIntVar(2, 6);
        exp = new IntExpPowIntExp(base, power);
        // only odd powers
        removeAllEven(power);
        // base becomes negative
        try {
            base.setMax(-1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("odd powers and negative base", (int) Math.pow(base.min(), power.max()), exp.min());

        // odd powers && changable base
        base = C.addIntVar(-15, 4);
        power = C.addIntVar(2, 6);
        exp = new IntExpPowIntExp(base, power);
        // only odd powers
        removeAllEven(power);
        assertEquals("odd powers && changable base", (int) Math.pow(base.min(), power.max()), exp.min());

        // even powers && changable base
        // 1)
        base = C.addIntVar(-15, 4);
        power = C.addIntVar(2, 6);
        exp = new IntExpPowIntExp(base, power);
        assertEquals((int) -Math.pow(15, 5), exp.min());
        // only even powers
        removeAllOdd(power);
        // minimum has increased!!! It's important
        assertEquals("even powers && changable base", 0, exp.min());
        // 2)
        base = C.addIntVar(-2, 21);
        power = C.addIntVar(2, 6);
        exp = new IntExpPowIntExp(base, power);
        // only odd powers
        removeAllOdd(power);
        assertEquals("even powers && changable base 2) ", 0, exp.min());

        // any powers && changable base
        base = C.addIntVar(-12, 21);
        power = C.addIntVar(2, 6);
        exp = new IntExpPowIntExp(base, power);
        assertEquals("any powers && changable base ", (int) Math.pow(base.min(), 5 /* power.max_odd() */), exp.min());

        // any powers && positive base
        base = C.addIntVar(-12, 21);
        power = C.addIntVar(2, 6);
        exp = new IntExpPowIntExp(base, power);
        try {
            base.setMin(1);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("any powers && positive base ", (int) Math.pow(base.min(), power.min()), exp.min());
        // any powers && negative base
        base = C.addIntVar(-12, 21);
        power = C.addIntVar(2, 6);
        exp = new IntExpPowIntExp(base, power);
        try {
            base.setMax(-2);
            power.removeValue(5);
            C.propagate();
        } catch (Failure f) {
            fail("test failed");
        }
        assertEquals("any powers && negative base ", (int) Math.pow(base.min(), 3 /* power.max_odd() */), exp.min());

    }

    public void testUsage() {
        IntVar base = C.addIntVar(-4, 2, "base"), power = C.addIntVar(0, 3, "power", IntVar.DOMAIN_PLAIN);
        IntExp exp = new IntExpPowIntExp(base, power);
        try {
            C.postConstraint(power.ne(1));
            C.postConstraint(power.ne(3));
            C.postConstraint(exp.lt(1));
        } catch (Failure f) {
            fail("test failed");
        }
        boolean flag = C.execute(new GoalGenerate(new IntExpArray(C, power, base)));
        assertTrue("failed to generate values for base", flag);
        assertEquals(0, base.min());
        // ------------------------------------------------------------------------
        base = C.addIntVar(-4, 2, "base");
        power = C.addIntVar(0, 3, "power", IntVar.DOMAIN_PLAIN);
        exp = new IntExpPowIntExp(base, power);
        try {
            C.postConstraint(power.ne(1));
            C.postConstraint(power.ne(3));
            C.postConstraint(base.ne(0));
            C.postConstraint(exp.lt(1));
            flag = C.execute(new GoalGenerate(new IntExpArray(C, power, base)));
            assertTrue(!flag);
        } catch (Failure f) {
        }
        // ------------------------------------------------------------------------
        base = C.addIntVar(-4, 2, "base");
        power = C.addIntVar(0, 3, "power", IntVar.DOMAIN_PLAIN);
        exp = new IntExpPowIntExp(base, power);
        try {
            C.postConstraint(exp.gt(9));
        } catch (Failure f) {
        }
        flag = C.execute(new GoalGenerate(new IntExpArray(C, power, base)));
        assertTrue(flag);
        assertEquals(-4, base.min());
        assertEquals(2, power.min());
        // ------------------------------------------------------------------------
        base = C.addIntVar(-4, 2, "base");
        power = C.addIntVar(0, 3, "power", IntVar.DOMAIN_PLAIN);
        exp = new IntExpPowIntExp(base, power);
        try {
            C.postConstraint(power.ne(2));
            C.postConstraint(exp.gt(8));
            flag = C.execute(new GoalGenerate(new IntExpArray(C, power, base)));
            assertTrue(!flag);
        } catch (Failure f) {
        }
    }

}