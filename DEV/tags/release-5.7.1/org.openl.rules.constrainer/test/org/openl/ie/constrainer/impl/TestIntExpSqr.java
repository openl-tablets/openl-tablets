package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.IntExpSqr;

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

public class TestIntExpSqr extends TestCase {
    private Constrainer C = new Constrainer("TestIntExpSqr");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntExpSqr.class));
    }

    public TestIntExpSqr(String name) {
        super(name);
    }

    public void testEventPropagation() {
        IntVar intvar = C.addIntVar(-12, 7, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp sqrexp = new IntExpSqr(intvar);
        assertEquals(0, sqrexp.min());
        assertEquals(144, sqrexp.max());
        int oldmin = intvar.min(), newmin;
        int oldmax = intvar.max(), newmax;
        // descending
        try {
            newmin = -3;
            intvar.setMin(newmin);
            C.propagate();
            assertEquals(0, sqrexp.min());
            assertEquals(49, sqrexp.max());

            newmin = 1;
            intvar.setMin(newmin);
            C.propagate();
            assertEquals(1, sqrexp.min());
            assertEquals(49, sqrexp.max());

            newmax = 3;
            intvar.setMax(newmax);
            C.propagate();
            assertEquals(1, sqrexp.min());
            assertEquals(9, sqrexp.max());

            /*
             * intvar.removeValue(2); C.propagate();
             * assertTrue(!sqrexp.contains(4));
             */
        } catch (Failure f) {
            fail("test failed");
        }

        // descending
        /*
         * try{ intvar = C.addIntVar(-2,5); sqrexp = new IntExpSqr(intvar);
         * sqrexp.setMax(22); C.propagate(); assertEquals(4, intvar.max());
         * assertEquals(-2, intvar.min());
         *
         * sqrexp.setMin(1); C.propagate(); assertEquals(1, intvar.min());
         * assertEquals(4, intvar.max());
         *
         * intvar.removeValue(3); sqrexp.setMax(9); C.propagate();
         * assertEquals(1, intvar.min()); assertEquals(2, intvar.max());
         * }catch(Failure f){fail("");}
         */
    }

    public void testIntExpSqr() {
        IntVar intvar = C.addIntVar(-10, 10, "intvar", IntVar.DOMAIN_BIT_FAST);
        IntExp exp = new IntExpSqr(intvar);
        IntExpArray array = new IntExpArray(C, intvar);
        Goal goalgen = new GoalGenerate(array);
        try {
            C.postConstraint(exp.eq(25));
            boolean flag = C.execute(goalgen);
            assertTrue(flag);
            assertTrue((intvar.value() == 5) || (intvar.value() == -5));
        } catch (Failure f) {
            fail("test failed");
        }
    }

    public void testMinMax() {
        // max < 0
        IntVar intvar = C.addIntVar(-12, -7);
        IntExp sqrexp = new IntExpSqr(intvar);
        assertEquals("\" max < 0 \"", 144, sqrexp.max());
        assertEquals("\" max < 0 \"", 49, sqrexp.min());

        // min< 0 <max
        // abs(min) > max
        intvar = C.addIntVar(-12, 7);
        sqrexp = new IntExpSqr(intvar);
        assertEquals("\"min< 0 <max : abs(min) > max\" case failed", 144, sqrexp.max());
        assertEquals("\"min< 0 <max : abs(min) > max\" case failed", 0, sqrexp.min());
        // abs(min) < max
        intvar = C.addIntVar(-3, 7);
        sqrexp = new IntExpSqr(intvar);
        assertEquals("\"min< 0 <max : abs(min) < max\" case failed", 49, sqrexp.max());
        assertEquals("\"min< 0 <max : abs(min) < max\" case failed", 0, sqrexp.min());

        // min > 0
        intvar = C.addIntVar(3, 12);
        sqrexp = new IntExpSqr(intvar);
        assertEquals("\" max < 0 \"", 144, sqrexp.max());
        assertEquals("\" max < 0 \"", 9, sqrexp.min());

    }
}