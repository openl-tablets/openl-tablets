package org.openl.ie.constrainer.impl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author unascribed
 * @version 1.0
 */

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.ConstraintConst;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFloatGenerate;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.ConstraintExpEqualsExp;
import org.openl.ie.constrainer.impl.ConstraintExpEqualsValue;
import org.openl.ie.constrainer.impl.ConstraintExpLessExp;
import org.openl.ie.constrainer.impl.ConstraintExpLessValue;
import org.openl.ie.constrainer.impl.ConstraintExpMoreValue;
import org.openl.ie.constrainer.impl.ConstraintExpNotValue;
import org.openl.ie.constrainer.impl.ConstraintFloatExpEqualsExp;
import org.openl.ie.constrainer.impl.ConstraintFloatExpEqualsValue;
import org.openl.ie.constrainer.impl.ConstraintFloatExpLessExp;
import org.openl.ie.constrainer.impl.ConstraintFloatExpLessValue;
import org.openl.ie.constrainer.impl.ConstraintFloatExpMoreValue;
import org.openl.ie.constrainer.impl.ConstraintFloatExpNotValue;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestConstraints extends TestCase {

    static class GoalCheckBoolExp extends GoalImpl {
        private IntBoolExp _exp = null;

        public GoalCheckBoolExp(IntBoolExp exp) {
            super(C);
            _exp = exp;
        }

        public Goal execute() {
            assertTrue("toIntBoolExp works wrong for " + _exp.getClass().getName(), _exp.isTrue());
            return null;
        }
    }

    static Constrainer C = new Constrainer("Testing constrainer");

    static IntVar a = C.addIntVar(0, 10), b = C.addIntVar(0, 10);
    static IntExpArray array = new IntExpArray(C, a, b);

    static IntExp linear = a.add(b), linear1 = a.sub(b), nonlinear = a.mul(b);

    static FloatVar fa = C.addFloatVar(-10, 10, "fa"), fb = C.addFloatVar(-10, 10, "fb");
    static FloatExpArray farray = new FloatExpArray(C, fa, fb);

    static FloatExp flinear = fa.add(fb), flinear1 = fa.sub(fb), fnonlinear = fa.mul(fb);

    static int offset = 2;
    static double doffset = 2.5;
    static private Goal genGoal = new GoalGenerate(array);

    static Constraint[] constraints = { new ConstraintConst(C, true), new ConstraintConst(C, false),

    new ConstraintExpEqualsExp(linear, linear1, offset), new ConstraintExpEqualsExp(linear, nonlinear, offset),

    new ConstraintExpEqualsValue(linear, offset), new ConstraintExpEqualsValue(nonlinear, offset),

    new ConstraintExpLessExp(linear, linear1, offset), new ConstraintExpLessExp(linear, nonlinear, offset),

    new ConstraintExpLessValue(linear, offset), new ConstraintExpLessValue(nonlinear, offset),

    new ConstraintExpMoreValue(linear, offset), new ConstraintExpMoreValue(nonlinear, offset),

    new ConstraintFloatExpEqualsExp(flinear, flinear1, doffset),
            new ConstraintFloatExpEqualsExp(flinear, fnonlinear, doffset),

            new ConstraintFloatExpEqualsValue(flinear, doffset),
            new ConstraintFloatExpEqualsValue(fnonlinear, doffset),

            new ConstraintFloatExpLessExp(flinear, flinear1, doffset),
            new ConstraintFloatExpLessExp(flinear, fnonlinear, doffset),

            new ConstraintFloatExpLessValue(flinear, doffset), new ConstraintFloatExpLessValue(fnonlinear, doffset),

            new ConstraintFloatExpMoreValue(flinear, doffset), new ConstraintFloatExpMoreValue(fnonlinear, doffset),

            new ConstraintExpNotValue(linear, offset), new ConstraintExpNotValue(nonlinear, offset),

            new ConstraintFloatExpNotValue(flinear, doffset), new ConstraintFloatExpNotValue(fnonlinear, doffset) };

    static private void checkBoolExp(IntBoolExp actual, IntBoolExp expected) {
        Goal ex = new GoalAnd(actual.asConstraint(), genGoal);
        C.execute(new GoalAnd(ex, new GoalCheckBoolExp(expected)), true);
    }

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestConstraints.class));
    }

    public TestConstraints(String name) {
        super(name);
    }

    /*
     * public void testToIntBoolExp(){ IntExpArray array = new IntExpArray(C, a,
     * b); FloatExpArray farray = new FloatExpArray(C, fa, fb); Goal gen = new
     * GoalAnd(new GoalGenerate(array), new GoalFloatGenerate(farray));
     *
     * for (int i=0;i<constraints.length;i++){ IntBoolExp exp =
     * constraints[i].toIntBoolExp(); Goal ex = new GoalAnd(exp.asConstraint(),
     * gen); C.execute(new GoalAnd(ex, new GoalCheckBoolExp(exp)), true); } }
     */

    public void testIsLinear() {
        assertTrue(constraints[0].isLinear());
        assertTrue(constraints[1].isLinear());
        for (int i = 2; i < constraints.length - 4; i++) {
            assertTrue("wrong for" + constraints[i].getClass().getName(), ((i % 2) == 1) ? !constraints[i].isLinear()
                    : constraints[i].isLinear());
        }
        for (int i = constraints.length - 4; i < constraints.length; i++) {
            assertTrue(!constraints[i].isLinear());
        }
    }

    public void testToIntBoolExpDetailed() {
        IntBoolExp exp = new ConstraintExpLessExp(nonlinear, linear, offset).toIntBoolExp();
        checkBoolExp(exp, nonlinear.lt(linear.add(offset)));

        exp = new ConstraintExpLessValue(linear, 1).toIntBoolExp();
        checkBoolExp(exp, linear.lt(1));

        exp = new ConstraintExpEqualsValue(linear, 5).toIntBoolExp();
        checkBoolExp(exp, linear.eq(5));

        exp = new ConstraintExpMoreValue(linear, 5).toIntBoolExp();
        checkBoolExp(exp, linear.gt(5));

        exp = new ConstraintExpNotValue(linear, 0).toIntBoolExp();
        checkBoolExp(exp, linear.ne(0));

        exp = new ConstraintExpEqualsExp(nonlinear, linear, 2).toIntBoolExp();
        checkBoolExp(exp, nonlinear.eq(linear.add(2)));

        genGoal = new GoalFloatGenerate(farray);

        /*
         * exp = new ConstraintFloatExpEqualsExp(fnonlinear, flinear,
         * 2).toIntBoolExp(); IntBoolExp expected =
         * fnonlinear.eq(flinear.add(2)); checkBoolExp(exp, expected);
         */

        exp = new ConstraintFloatExpEqualsValue(fnonlinear, 8).toIntBoolExp();
        checkBoolExp(exp, fnonlinear.eq(8));

        exp = new ConstraintFloatExpLessExp(fnonlinear, flinear, 10).toIntBoolExp();
        checkBoolExp(exp, fnonlinear.lt(flinear.add(10)));

        exp = new ConstraintFloatExpLessValue(fnonlinear, -5).toIntBoolExp();
        checkBoolExp(exp, fnonlinear.lt(-5));

        exp = new ConstraintFloatExpMoreValue(flinear, 7).toIntBoolExp();
        checkBoolExp(exp, flinear.ge(7 - Constrainer.precision()));

        exp = new ConstraintFloatExpNotValue(flinear, -20).toIntBoolExp();
        checkBoolExp(exp, flinear.ne(-20 - Constrainer.precision()));

        exp = new ConstraintFloatExpNotValue(flinear, -20).toIntBoolExp();
        checkBoolExp(exp, flinear.ne(-20 - Constrainer.precision()));
    }
}
