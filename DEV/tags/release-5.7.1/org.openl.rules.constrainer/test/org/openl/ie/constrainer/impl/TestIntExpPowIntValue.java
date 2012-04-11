package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalInstantiate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntExpPowIntValue;

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

public class TestIntExpPowIntValue extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpPowIntValue");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpPowIntValue.class));
    }

    public TestIntExpPowIntValue(String name) {
        super(name);
    }

    public void testBound() {
        int exponent = 2;
        IntVar intvar = C.addIntVar(-5, 5, "base", IntVar.DOMAIN_BIT_FAST);
        IntExp exp = new IntExpPowIntValue(intvar, exponent);
        assertTrue(!exp.bound());
        try {
            intvar.setMin(3);
            C.propagate();
            assertEquals("doesn't respond to base.setMin(int)", 9, exp.min());
            assertEquals("incorrect response to base.setMin(int)", 25, exp.max());
            intvar.setMax(3);
            C.propagate();
            assertEquals("doesn't respond to base.setMin(int)", 9, exp.max());
            assertTrue(exp.bound());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testFunctionality() {

        class IntegerValue {
            private int _value = 0;

            public int value() {
                return _value;
            }

            public void value(int value) {
                _value = value;
            }
        }

        final IntegerValue lastResult = new IntegerValue();

        class GoalSaveSolution extends GoalImpl {
            private IntExp _exp = null;
            private IntegerValue _storage = null;

            public GoalSaveSolution(IntExp exp, IntegerValue storage) {
                super(exp.constrainer());
                _exp = exp;
                _storage = storage;
            }

            public Goal execute() throws Failure {
                assertTrue("GoalSaveSolution found it's argument to be unbound", _exp.bound());
                lastResult.value(_exp.value());
                return null;
            }
        }

        IntVar base = C.addIntVar(-4, 4);
        int exponent = 3;
        int[] pow = { -64, -27, -8, -1, 0, 1, 8, 27, 64 };
        IntExpPowIntValue exp = new IntExpPowIntValue(base, exponent);

        for (int i = 0; i < pow.length; i++) {
            Goal g = new GoalAnd(exp.equals(pow[i]), new GoalInstantiate(base), new GoalSaveSolution(base, lastResult));
            C.execute(g, true);
            assertEquals(-4 + i, lastResult.value());
        }
    }

    public void testIntPowIntValue() {
        int exponent = 2;
        IntVar intvar = C.addIntVar(-5, 5, "base", IntVar.DOMAIN_BIT_FAST);
        IntExp exp = new IntExpPowIntValue(intvar, exponent);
        IntExpArray array = new IntExpArray(C, intvar);
        GoalGenerate goalgen = new GoalGenerate(array);

        try {
            C.postConstraint(exp.eq(9));
            boolean flag = C.execute(goalgen);
            assertTrue(flag);
            assertEquals(9, exp.value());
        } catch (Failure f) {
            fail("test failed!");
        }
    }

    public void testMinMaxContains() {
        int exponent = -2;
        IntVar base = C.addIntVar(-10, 10, "base", IntVar.DOMAIN_BIT_FAST);
        IntExp exp = new IntExpPowIntValue(base, exponent);
        /*
         * assertEquals(0, exp.min()); assertEquals(Math.pow(1, -2), exp.max(),
         * Double.MIN_VALUE);
         *
         * exponent = -1; base = C.addIntVar(-10, 10, "base",
         * IntVar.DOMAIN_BIT_FAST); exp = new IntExpPowIntValue(base, exponent);
         * assertEquals(0, exp.min()); assertEquals(Math.pow(1, -1), exp.max(),
         * Double.MIN_VALUE);
         *
         */
        exponent = 0;
        base = C.addIntVar(-10, -1, "base", IntVar.DOMAIN_BIT_FAST);
        exp = new IntExpPowIntValue(base, exponent);
        assertEquals(1, exp.min());
        assertEquals(1, exp.max());

        exponent = 1;
        base = C.addIntVar(-10, 10, "base", IntVar.DOMAIN_BIT_FAST);
        exp = new IntExpPowIntValue(base, exponent);
        assertEquals(-10, exp.min());
        assertEquals(10, exp.max());

        exponent = 1;
        base = C.addIntVar(-10, 10, "base", IntVar.DOMAIN_BIT_FAST);
        exp = new IntExpPowIntValue(base, exponent);
        assertEquals(-10, exp.min());
        assertEquals(10, exp.max());

        exponent = 2;
        base = C.addIntVar(-10, 10, "base", IntVar.DOMAIN_BIT_FAST);
        exp = new IntExpPowIntValue(base, exponent);
        assertEquals(0, exp.min());
        assertEquals(100, exp.max());

        exponent = 3;
        base = C.addIntVar(-10, 10, "base", IntVar.DOMAIN_BIT_FAST);
        exp = new IntExpPowIntValue(base, exponent);
        assertEquals(-1000, exp.min());
        assertEquals(1000, exp.max());

        /*
         * IntVar intvar = C.addIntVar(-15,5,"intvar", IntVar.DOMAIN_BIT_FAST);
         * IntExp powexp = new IntExpOpposite(intvar); int size = oppexp.size();
         * int minVal = oppexp.min(); int maxVal = oppexp.max(); try{
         * //increasing minimum for (int i=minVal;i<maxVal;i++){
         * oppexp.setMin(i); C.propagate();
         *
         * assertEquals(intvar.min(), -oppexp.max()); assertEquals(intvar.max(),
         * -oppexp.min()); assertEquals(intvar.size(), oppexp.size());
         *
         * for (int j=-minVal;j>-i;j--){ assertTrue(!intvar.contains(j)); } for
         * (int j=-maxVal;j<=-i;j++){ assertTrue(intvar.contains(j)); } }
         *
         * //decreasing maximum intvar = C.addIntVar(-maxVal,-minVal,"intvar",
         * IntVar.DOMAIN_BIT_FAST); oppexp = new IntExpOpposite(intvar); for
         * (int i=0;i<maxVal-minVal;i++){ int newMax = maxVal-i;
         * oppexp.setMax(newMax); C.propagate();
         *
         * assertEquals(intvar.min(), -oppexp.max()); assertEquals(intvar.max(),
         * -oppexp.min()); assertEquals(intvar.size(), oppexp.size());
         *
         * for (int j=-maxVal;j<-newMax;j++){ assertTrue("contains "+
         * j,!intvar.contains(j)); } for (int j=-minVal;j>=-newMax;j--){
         * assertTrue("doesn't contain " + j,intvar.contains(j)); } }
         *
         * //successively removing values intvar =
         * C.addIntVar(-maxVal,-minVal,"intvar", IntVar.DOMAIN_BIT_FAST); oppexp =
         * new IntExpOpposite(intvar); for (int i=minVal;i<maxVal;i++){
         * oppexp.removeValue(i); C.propagate();
         *
         * assertEquals(intvar.min(), -oppexp.max()); assertEquals(intvar.max(),
         * -oppexp.min()); assertEquals(intvar.size(), oppexp.size());
         *
         * for (int j=-minVal;j>=-i;j--){ assertTrue(!intvar.contains(j)); } for
         * (int j=-maxVal;j<-i;j++){ assertTrue(intvar.contains(j)); } }
         *  } catch(Failure f){fail("test failed");}
         */
    }
}