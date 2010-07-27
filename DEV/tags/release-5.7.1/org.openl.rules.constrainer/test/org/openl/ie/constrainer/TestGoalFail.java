package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalFail;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestGoalFail extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(new TestSuite(TestGoalFail.class));
    }

    public TestGoalFail(String name) {
        super(name);
    }

    public void testExecute() {
        Constrainer C = new Constrainer("test GoalFail");
        Goal gfail = new GoalFail(C);
        try {
            gfail.execute();
            fail("Goal fail doesn't work properly!!!");
        } catch (Failure f) {
        } catch (Throwable ex) {
            fail("Unexpected exception:" + ex);
        }
    }
}