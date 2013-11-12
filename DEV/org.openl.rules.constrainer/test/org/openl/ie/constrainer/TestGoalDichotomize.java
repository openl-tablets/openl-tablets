package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalDichotomize;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestGoalDichotomize extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(new TestSuite(TestGoalDichotomize.class));
    }

    public TestGoalDichotomize(String name) {
        super(name);
    }

    public void testExcute() {
        Constrainer C = new Constrainer("TestGoalAnd");
        IntVar a = C.addIntVar(0, 10, "a", IntVar.DOMAIN_BIT_FAST);
        IntVar b = C.addIntVar(0, 10, "b", IntVar.DOMAIN_BIT_FAST);
        IntExpArray arr_a = new IntExpArray(C, a);
        IntExpArray arr_b = new IntExpArray(C, b);
        try {
            C.postConstraint(a.add(b).gt(4));
            C.postConstraint(a.add(b).lt(6));
            C.postConstraint(a.le(b));
            C.postConstraint(a.mul(b).eq(6));
            Goal gen = new GoalDichotomize(a, true);
            boolean flag = C.execute(gen);
            assertTrue("Can't generate any solution", flag);
            /**
             * the only solution is a=2, b=3
             */
            assertEquals("a = " + a.value(), a.value(), 2);
        } catch (Failure f) {
            fail("GoalDichotomize test failed: " + f);
            f.printStackTrace();
        }
        try {
            a = C.addIntVar(0, 10, "a", IntVar.DOMAIN_BIT_FAST);
            b = C.addIntVar(0, 4, "b", IntVar.DOMAIN_BIT_FAST);
            a.removeValue(5);
            C.postConstraint(a.mul(b).eq(5));

            Goal gen = new GoalAnd(new GoalDichotomize(a, true), new GoalDichotomize(b, true));
            boolean flag = C.execute(gen);
            assertTrue("Can't generate any solution", !flag);
        } catch (Failure f) {
            fail("GoalDichotomize test failed: " + f);
            f.printStackTrace();
        }
    }
}