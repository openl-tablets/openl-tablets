package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestGoalFastMinimize extends TestCase {
    private Constrainer C = new Constrainer("Test of GoalFastMinimize");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestGoalFastMinimize.class));
    }

    public TestGoalFastMinimize(String name) {
        super(name);
    }

    public void testExecute() {
        try {
            IntVar x = C.addIntVar(-100, 100, "x", IntVar.DOMAIN_PLAIN);
            IntVar y = C.addIntVar(-100, 100, "y", IntVar.DOMAIN_PLAIN);
            IntExpArray yx = new IntExpArray(C, x, y);

            C.postConstraint(y.ge(x.sqr()));
            C.postConstraint(y.ge(x.add(2)));
            IntExp cost = y.sub(x);

            Goal minimize = new GoalFastMinimize(new GoalGenerate(yx), cost);
            boolean flag = C.execute(minimize);
            assertTrue("Can't minimize an objective function!", flag);
            assertEquals(1, y.value());
            assertEquals(-1, x.value());

        } catch (Failure f) {
            f.printStackTrace();
        }
        try {
            IntVar x = C.addIntVar(-10, 10, "x1", IntVar.DOMAIN_PLAIN);
            IntVar y = C.addIntVar(-10, 10, "y1", IntVar.DOMAIN_PLAIN);
            IntExpArray yx = new IntExpArray(C, x, y);

            C.postConstraint(y.ge(x.sqr()));
            C.postConstraint(y.le(x.add(2)));
            IntExp cost = (x.sqr().sub(y.sqr()));

            Goal minimize = new GoalFastMinimize(new GoalGenerate(yx), cost);
            boolean flag = C.execute(minimize);
            assertTrue("Can't minimize the objective function!", flag);
            assertEquals(4, y.value());
            assertEquals(2, x.value());
        } catch (Failure f) {
            f.printStackTrace();
        }
        try {
            // VolsayProduction
            IntVar x = C.addIntVar(0, 500, "gas", IntVar.DOMAIN_PLAIN);
            IntVar y = C.addIntVar(0, 500, "chloride", IntVar.DOMAIN_PLAIN);
            IntExpArray yx = new IntExpArray(C, x, y);

            C.postConstraint(x.add(y).le(50));
            C.postConstraint(x.mul(3).add(y.mul(4)).le(180));
            C.postConstraint(y.le(40));
            IntExp cost = (x.mul(40).add(y.mul(50))).neg();

            Goal minimize = new GoalFastMinimize(new GoalGenerate(yx), cost);
            boolean flag = C.execute(minimize);
            assertTrue("Can't minimize the objective function!", flag);
            assertEquals(30, y.value());
            assertEquals(20, x.value());
        } catch (Failure f) {
            f.printStackTrace();
        }
    }
}