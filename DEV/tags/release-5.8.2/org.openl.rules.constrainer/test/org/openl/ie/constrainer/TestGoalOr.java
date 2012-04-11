package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalFail;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalOr;
import org.openl.ie.constrainer.GoalSetMax;
import org.openl.ie.constrainer.GoalSetMin;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestGoalOr extends TestCase {
    private static Constrainer C = new Constrainer("GoalOr test");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestGoalOr.class));
    }

    public TestGoalOr(String name) {
        super(name);
    }

    public void testExecute() {

        class GoalSuccess extends GoalImpl {
            GoalSuccess() {
                super(C, "GoalSuccess");
            }

            public Goal execute() throws Failure {
                return null;
            }
        }

        class GoalFailTest extends GoalImpl {
            private String _str = null;

            GoalFailTest(String str) {
                super(C, "");
                _str = str;
            }

            public Goal execute() throws Failure {
                fail(_str);
                return null;
            }
        }

        IntVar var = C.addIntVar(0, 10, "var", IntVar.DOMAIN_PLAIN);
        String[] errorString = {
                "Test failed: both subgoals of GoalOr has been executed despite"
                        + "the fact that it's first goal succeeded",

                "Test failed: first subgoal of GoalOr hasn't been executed",

                "Test failed: second subgoal of GoalOr hasn't been executed despite"
                        + "the fact that it's first goal failed" };
        // the case when the first subgoal is to be succeeded
        assertTrue(C.execute(new GoalOr(new GoalSetMin(var, 2), new GoalSetMax(var, 8))));
        assertTrue(errorString[1], (var.min() == 2));
        assertTrue(errorString[0], (var.max() == 10));
        // the case when the first subgoal fails and the second is to be
        // succeeded
        assertTrue(C.execute(new GoalOr(new GoalFail(C), new GoalSetMax(var, 8))));
        assertTrue(errorString[2], (var.max() == 8));
        // the case when the first subgoal is to be succeeded
        assertTrue(C.execute(new GoalOr(new GoalSuccess(), new GoalFailTest(errorString[0]))));
        // the case when both subgoals are to fail
        assertTrue(!C.execute(new GoalOr(new GoalFail(C), new GoalFail(C))));
    }

}