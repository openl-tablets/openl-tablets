package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFail;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalInstantiate;
import org.openl.ie.constrainer.GoalSetMax;
import org.openl.ie.constrainer.GoalSetMin;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestGoalAnd extends TestCase {
    Constrainer C = new Constrainer("TestGoalAnd");

    public static void main(String[] args) {
        junit.textui.TestRunner.run(new TestSuite(TestGoalAnd.class));
    }

    public TestGoalAnd(String name) {
        super(name);
    }

    public void testExecute() {
        IntVar a = C.addIntVar(0, 10, "a", IntVar.DOMAIN_PLAIN);
        IntVar b = C.addIntVar(0, 10, "b", IntVar.DOMAIN_PLAIN);
        try {
            C.postConstraint(a.add(b).gt(4));
            C.postConstraint(a.add(b).lt(6));
            C.postConstraint(a.le(b));
            C.postConstraint(a.mul(b).eq(6));
            Goal gen = new GoalAnd(new GoalInstantiate(a), new GoalInstantiate(b));
            boolean flag = C.execute(gen);
            assertTrue("Can't generate any solution", flag);
            assertEquals("a = " + a.value(), a.value(), 2);
            assertEquals("b = " + b.value(), b.value(), 3);
        } catch (Failure f) {
            f.printStackTrace();
        } catch (Throwable ex) {
            fail("Unexpected exception has been thrown");
        }
    }

    public void testExecute1() {
        IntVar var = C.addIntVar(0, 10, "var", IntVar.DOMAIN_PLAIN);

        class GoalSuccess extends GoalImpl {
            GoalSuccess() {
                super(C, "GoalSuccess");
            }

            public Goal execute() throws Failure {
                return null;
            }
        }

        class GoalOutput extends GoalImpl {
            private String _str = null;

            GoalOutput(String str) {
                super(C, "");
                _str = str;
            }

            public Goal execute() throws Failure {
                fail(_str);
                return null;
            }
        }

        assertTrue(C.execute(new GoalAnd(new GoalSetMin(var, 2), new GoalSetMax(var, 8))));
        assertTrue((var.min() == 2) && (var.max() == 8));
        assertTrue(!C.execute(new GoalAnd(new GoalSetMin(var, 5), new GoalSetMax(var, 4))));
        assertTrue(!C.execute(new GoalAnd(new GoalFail(C), new GoalSuccess())));
        assertTrue(!C.execute(new GoalAnd(new GoalFail(C), new GoalOutput("Test Failed:"
                + "Both subgoals of GoalAnd has been executed despite the fact that the first subgoal failed"))));
    }
}