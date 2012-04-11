package org.openl.ie.constrainer.impl;

import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFloatGenerate;
import org.openl.ie.constrainer.GoalInstantiate;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExpConst;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigen Group, Inc.
 * </p>
 */

public class AnotherFloatVarTest extends TestCase {
    private Vector _constraints = null;

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(AnotherFloatVarTest.class));
    }

    public AnotherFloatVarTest(String name) {
        super(name);
    }

    private void addConstraint(Constrainer C, int num, double weight) {
        if (_constraints == null) {
            return;
        }
        if (Math.random() > (1 - weight)) {
            // outputs are committed not to spoil the log
//            System.out.print("\tC" + num);
            IntBoolExp ex = (IntBoolExp) _constraints.get(num);
            C.addConstraint(ex);
        }
    }

    public void testAll() {
        for (int i = 0; i < 200; i++) {
            Constrainer C = new Constrainer("AnotherFloatVarTest");
            _constraints = new Vector();
            Goal g1 = null, g2 = null;
            FloatVar x = null, y = null;
            IntVar k = null;
            try {
                int counter = 0;
                // declaring variables
                x = C.addFloatVar(-5, 1e+05, "x");
                y = C.addFloatVar(0, 1e+05, "y");
                k = C.addIntVar(1, 1000, "k");
                // creating constraints
                _constraints.add((x.pow(3).add(x.mul(10)).eq(y.pow(x).sub(new IntExpConst(C, 2).pow(k)))));
                _constraints.add(x.mul(k).add(y.mul(7.7)).eq(2.4));
                _constraints.add((x.sub(1).div(y.add(1))).abs().lt(5));
                _constraints.add(y.ge((k.pow(2)).asFloat()).implies(x.le(0).and(y.le(1))));
                _constraints.add(x.le(0).implies(k.lt(7)));
                // preparing goals
                FloatExpArray vars = new FloatExpArray(C, x, y);
                g1 = new GoalFloatGenerate(vars);
                g2 = new GoalInstantiate(k);

//                System.out.print("\nactivated constraints: ");

                for (int ci = 0; ci < _constraints.size(); ci++) {
                    addConstraint(C, ci, 0.7);
                }
                /*
                 * addConstraint(C, 0, 1); addConstraint(C, 1, 1);
                 * addConstraint(C, 2, 1); addConstraint(C, 3, 1);
                 * addConstraint(C, 4, 1);
                 */

            } catch (Failure f) {
                fail("FloatExp.pow() failed");
            }
            try {
                C.postConstraints();
//                System.out.println("\nsuccessfully posted");
//                System.out.println("current domains:" + i + "\nx = " + x + "\ny = " + y + "\nk = " + k);
            } catch (Failure f) {
                fail("constraints: the problem is inconsistent");
            }

            boolean flag = C.execute(new GoalAnd(g1, g2));
            assertTrue("the problem is inconsistent", flag);
//            System.out.println("\nSolution number" + i + "\nx = " + x + "\ny = " + y + "\nk = " + k);
        }
    }
}