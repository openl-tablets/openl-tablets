package org.openl.ie.constrainer;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalMinimize;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestGoalMinimize extends TestCase {
    Constrainer C = new Constrainer("test GoalMinimize");

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestGoalMinimize.class));
    }

    public TestGoalMinimize(String name) {
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

    public void testKnapsackProblem() {
        try {
            int nbResources = 7;
            int nbItems = 12;
            // int[] capacity= {18209, 7692, 1333, 924, 26638, 61188, 13360};
            int[] capacity = { 1820, 769, 133, 924, 2663, 6118, 1336 };
            int[] value = { 96, 76, 56, 11, 86, 10, 66, 86, 83, 12, 9, 81 };
            int[][] use = { { 19, 1, 10, 1, 1, 14, 152, 11, 1, 1, 1, 1 }, { 0, 4, 53, 0, 0, 80, 0, 4, 5, 0, 0, 0 },
                    { 4, 660, 3, 0, 30, 0, 3, 0, 4, 90, 0, 0 }, { 7, 0, 18, 6, 770, 330, 7, 0, 0, 6, 0, 0 },
                    { 0, 20, 0, 4, 52, 3, 0, 0, 0, 5, 4, 0 }, { 0, 0, 40, 70, 4, 63, 0, 0, 60, 0, 4, 0 },
                    { 0, 32, 0, 0, 0, 5, 0, 3, 0, 660, 0, 9 } };
            Constrainer C1 = new Constrainer("knapsack problem");
            // variables
            IntExpArray take = new IntExpArray(C1, nbItems, 0, 61188, "take");
            // costFunction
            IntExp costFunc = C1.scalarProduct(take, value);
            // capacity due constraints
            for (int r = 0; r < nbResources; r++) {
                C1.postConstraint(C1.scalarProduct(take, use[r]).le(capacity[r]));
            }

            GoalMinimize gm = new GoalMinimize(new GoalGenerate(take), costFunc.neg(), false);
            boolean flag = false;
            flag = C1.execute(gm, false);
            assertTrue("the solution hasn't been found", flag);
            assertTrue("cost function is still unbounded", costFunc.bound());
            // assertEquals("The solution isn't an optimal one", -26161,
            // costFunc.max());
            /*
             * System.out.println("Variables: "); for (int i=0;i<take.size();i++){
             * System.out.println("take["+i+"] = " + take.get(i).value()); }
             */
        } catch (Failure f) {
            fail("test failed");
        }
    } // end of testKnapsackProblem

    public void testKnapsackProblemFast() {
        try {
            int nbResources = 7;
            int nbItems = 12;
            // int[] capacity= {18209, 7692, 1333, 924, 26638, 61188, 13360};
            int[] capacity = { 1820, 769, 133, 924, 2663, 6118, 1336 };
            int[] value = { 96, 76, 56, 11, 86, 10, 66, 86, 83, 12, 9, 81 };
            int[][] use = { { 19, 1, 10, 1, 1, 14, 152, 11, 1, 1, 1, 1 }, { 0, 4, 53, 0, 0, 80, 0, 4, 5, 0, 0, 0 },
                    { 4, 660, 3, 0, 30, 0, 3, 0, 4, 90, 0, 0 }, { 7, 0, 18, 6, 770, 330, 7, 0, 0, 6, 0, 0 },
                    { 0, 20, 0, 4, 52, 3, 0, 0, 0, 5, 4, 0 }, { 0, 0, 40, 70, 4, 63, 0, 0, 60, 0, 4, 0 },
                    { 0, 32, 0, 0, 0, 5, 0, 3, 0, 660, 0, 9 } };
            Constrainer C1 = new Constrainer("knapsack problem");
            // variables
            IntExpArray take = new IntExpArray(C1, nbItems, 0, 6118, "take");
            // costFunction
            IntExp costFunc = C1.scalarProduct(take, value);
            // capacity due constraints
            for (int r = 0; r < nbResources; r++) {
                C1.postConstraint(C1.scalarProduct(take, use[r]).le(capacity[r]));
            }

            GoalFastMinimize gm = new GoalFastMinimize(new GoalGenerate(take), costFunc.neg(), false);
            boolean flag = false;
            flag = C1.execute(gm, false);
            assertTrue("the solution hasn't been found", flag);
            assertTrue("cost function is still unbounded", costFunc.bound());
            System.out.println(costFunc.max());
            // assertEquals("The solution isn't an optimal one", -26161,
            // costFunc.max());
            /*
             * System.out.println("Variables: "); for (int i=0;i<take.size();i++){
             * System.out.println("take["+i+"] = " + take.get(i).value()); }
             */
        } catch (Failure f) {
            fail("test failed");
        }
    } // end of testKnapsackProblem
}