package org.openl.ie.constrainer.impl;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author unascribed
 * @version 1.0
 */
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import org.openl.ie.constrainer.ChoicePointLabel;
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalMinimize;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.IntVarSelector;
import org.openl.ie.constrainer.IntVarSelectorMinSize;
import org.openl.ie.constrainer.IntVarSelectorMinSizeMin;
import org.openl.ie.constrainer.Session;
import org.openl.ie.constrainer.TimeLimitException;
import org.openl.ie.scheduler.AlternativeResourceSet;
import org.openl.ie.scheduler.GoalSetTimes;
import org.openl.ie.scheduler.Job;
import org.openl.ie.scheduler.JobVariableSelector;
import org.openl.ie.scheduler.Resource;
import org.openl.ie.scheduler.Schedule;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestExecutionControl2 extends TestCase {
    public static final class TwoOvens {
        static class MySelector implements JobVariableSelector {
            public IntVarSelector getSelector(IntExpArray vars) {
                return new IntVarSelectorMinSize(vars);
            }
        }

        final static int OVENS = 2;

        public static String main(int timeout) throws Exception {
            Constrainer C = new Constrainer("TwoOvens Scheduling Example");
            Schedule S = new Schedule(C, 0, 11);
            S.setName("TwoOvens");

            long executionStart = System.currentTimeMillis();

            Job jA = S.addJob(1, "JobA");
            Job jB = S.addJob(4, "JobB");
            Job jC = S.addJob(4, "JobC");
            Job jD = S.addJob(2, "JobD");
            Job jE = S.addJob(4, "JobE");
            Job jF = S.addJob(1, "JobF");
            Job jG = S.addJob(3, "JobG");
            Job jH = S.addJob(3, "JobH");
            Job jI = S.addJob(2, "JobI");
            Job jJ = S.addJob(1, "JobJ");

            Job j1 = S.addJob(1, "Job1");
            Job j2 = S.addJob(2, "Job2");
            Job j3 = S.addJob(3, "Job3");
            Job j4 = S.addJob(4, "Job4");

            // creating resources for two ovens
            AlternativeResourceSet res = new AlternativeResourceSet();

            Resource oven1 = S.addResourceDiscrete(3, "oven1");
            Resource oven2 = S.addResourceDiscrete(3, "oven2");
            Resource oven3 = S.addResourceDiscrete(2, "oven3");

            res.add(oven1);
            res.add(oven2);
            res.add(oven3);

            oven1.setCapacityMax(0, 1, 2);
            oven1.setCapacityMax(1, 2, 1);
            oven1.setCapacityMax(2, 3, 0);
            oven1.setCapacityMax(3, 5, 1);
            oven1.setCapacityMax(5, 10, 3);
            oven1.setCapacityMax(10, 11, 1);

            oven2.setCapacityMax(0, 2, 1);
            oven2.setCapacityMax(2, 5, 2);
            oven2.setCapacityMax(5, 7, 1);
            oven2.setCapacityMax(7, 8, 0);
            oven2.setCapacityMax(8, 11, 2);

            oven3.setCapacityMax(0, 2, 1);
            oven3.setCapacityMax(2, 5, 2);
            oven3.setCapacityMax(5, 7, 1);
            oven3.setCapacityMax(7, 8, 1);
            oven3.setCapacityMax(8, 11, 2);

            C.postConstraint(j3.requires(res, 1));
            C.postConstraint(j4.requires(res, 1));

            C.postConstraint(jB.requires(res, 1));
            C.postConstraint(jC.requires(res, 1));
            C.postConstraint(jD.requires(res, 1));
            C.postConstraint(jF.requires(res, 1));
            C.postConstraint(jG.requires(res, 1));
            C.postConstraint(jI.requires(res, 1));
            C.postConstraint(jA.requires(res, 2));
            C.postConstraint(j1.requires(res, 2));
            C.postConstraint(j2.requires(res, 2));
            C.postConstraint(jE.requires(res, 2));
            C.postConstraint(jH.requires(res, 2));
            C.postConstraint(jJ.requires(res, 3));

            Goal solution = new GoalSetTimes(S.jobs(), new MySelector());

            long solveStart = System.currentTimeMillis();

            // C.traceExecution();
            // C.traceFailures();
            // C.trace(vars);

            C.printInformation();
            Goal gm = solution;
            executeWithTimeout(C, solution, timeout);

            System.out.println(oven1);
            System.out.println(oven2);
            System.out.println(oven3);

            long solveTime = System.currentTimeMillis() - solveStart;
            long executionTime = System.currentTimeMillis() - executionStart;

            System.out.println("Execution time: " + executionTime + " msec");
            System.out.println("Solving time: " + solveTime + " msec");

            return oven1.toString() + oven2.toString() + oven3.toString();
        }

    }
    private final String TEST_FILE = "\\test.serialize";

    private IntExpArray x, xplus, xminus;

    private static boolean executeWithTimeout(Constrainer C, Goal gm, long timeout) {
        Object lastLabel = null;
        boolean flag = false;
        int timeouts = 0;
        C.setTimeLimit(timeout);
        Session s = new Session(C, null, null, gm);
        while (true) {
            try {
                flag = s.execute(false);
                break;
            } catch (TimeLimitException timelim) {
                lastLabel = timelim.label();
                timeouts++;
                System.out.print("<" + timeouts + ">");
            }
        }
        System.out.println("\nTotal exceedances: " + timeouts);
        return flag;
    }

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestExecutionControl2.class));
        double d = Constrainer.FLOAT_MAX;
    }

    public TestExecutionControl2(String name) {
        super(name);
    }

    private void checkOutChessProblemSolution() {
        int[] xvals = new int[x.size()];
        for (int i = 0; i < xvals.length; i++) {
            assertTrue(i + "'th variable is still unbounded", x.get(i).bound());
            xvals[i] = x.get(i).max();
        }
        assertTrue("There are queens situated on the same vertical", TestUtils.isAllDiff(xvals));

        for (int i = 0; i < xvals.length; i++) {
            assertTrue("xplus", xplus.get(i).bound());
            xvals[i] = xplus.get(i).max();
        }
        assertTrue("There are queens situated on the same diagonal", TestUtils.isAllDiff(xvals));

        for (int i = 0; i < xvals.length; i++) {
            assertTrue("xminus", xminus.get(i).bound());
            xvals[i] = xminus.get(i).max();
        }
        assertTrue("There are queens situated on the same diagonal", TestUtils.isAllDiff(xvals));
    }

    private void checkOutQueenProblemSolution(IntVar[] solution) {
        int[] xvals = new int[solution.length];
        for (int i = 0; i < solution.length; i++) {
            assertTrue(i + "'th variable is still unbounded", solution[i].bound());
            xvals[i] = solution[i].max();
        }
        assertTrue("There are queens situated on the same vertical", TestUtils.isAllDiff(xvals));
        int[] tmp = new int[xvals.length];
        for (int i = 0; i < solution.length; i++) {
            tmp[i] = xvals[i] + i;
        }
        assertTrue("There are queens situated on the same diagonal", TestUtils.isAllDiff(tmp));
        for (int i = 0; i < solution.length; i++) {
            tmp[i] = xvals[i] - i;
        }
        assertTrue("There are queens situated on the same diagonal", TestUtils.isAllDiff(tmp));
    }

    private void prepareChessProblem(org.openl.ie.constrainer.Constrainer C, int num) throws Failure {

        // board size and simultaneously the number of queens
        int board_size = num;
        // array of queens rows
        x = new IntExpArray(C, board_size);
        // auxillary arrays
        xplus = new IntExpArray(C, board_size);
        xminus = new IntExpArray(C, board_size);

        for (int i = 0; i < board_size; i++) {
            IntVar variable = C.addIntVar(0, board_size - 1, "q" + i, IntVar.DOMAIN_BIT_SMALL);
            x.set(variable, i);
            xplus.set(variable.add(i), i);
            xminus.set(variable.sub(i), i);
        }

        // all rows are different
        C.postConstraint(C.allDiff(x));
        // x[i] + i != x[j] + j
        C.postConstraint(C.allDiff(xplus));
        // x[i] - i != x[j] - j
        C.postConstraint(C.allDiff(xminus));
        C.printInformation();
    }

    private Session restoreState() {
        Session s = null;
        try {
            ObjectInputStream out = new ObjectInputStream(new FileInputStream(TEST_FILE));
            // C1 = (Constrainer)out.readObject();
            s = new Session(out);
            out.close();
        } catch (StreamCorruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return s;
    }

    private void saveState(Constrainer C, IntVar[] ints, FloatVar[] floats, Goal maingoal) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(TEST_FILE));
            Session s = new Session(C, ints, floats, maingoal);
            s.store(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void testCloseAndRestoreSession() {
        System.out.println("testCloseAndRestoreSession");
        Constrainer C = new Constrainer("TestConstraintExpEqualsExp");
        Object label = null;
        try {
            prepareChessProblem(C, 500);
            C.setTimeLimit(4);
            try {
                C.execute(new GoalGenerate(x, new IntVarSelectorMinSizeMin(x), true), false);
            } catch (TimeLimitException ex) {
                label = ex.label();
                IntVar[] arr = new IntVar[x.size()];
                for (int i = 0; i < x.size(); i++) {
                    arr[i] = (IntVar) x.get(i);
                }
                saveState(C, arr, null, null);
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

        // restore
        Session s = restoreState();
        Constrainer C1 = s.getConstrainer();

        C1.setTimeLimit(0);
        boolean flag = s.execute(false);

        IntVar[] queens = s.getIntVarsOfInterest();
        checkOutQueenProblemSolution(queens);

    }

    public void testMinimizeGoalsRestoration() {
        final String[] gn = { "FastMinimize", "Minimize" };
        // knapsack problem
        for (int N = 0; N < 2; N++) // fastminimize and minimize goals
        {
            System.out.print("testMinimizeGoalsRestoration: ");
            try {
                int nbResources = 7;
                int nbItems = 12;
                // int[] capacity= {18209, 7692, 1333, 924, 26638, 61188,
                // 13360};
                int[] capacity = { 1820, 769, 133, 924, 2663, 6118, 1336 };
                int[] value = { 96, 76, 56, 11, 86, 10, 66, 86, 83, 12, 9, 81 };
                int[][] use = { { 19, 1, 10, 1, 1, 14, 152, 11, 1, 1, 1, 1 }, { 0, 4, 53, 0, 0, 80, 0, 4, 5, 0, 0, 0 },
                        { 4, 660, 3, 0, 30, 0, 3, 0, 4, 90, 0, 0 }, { 7, 0, 18, 6, 770, 330, 7, 0, 0, 6, 0, 0 },
                        { 0, 20, 0, 4, 52, 3, 0, 0, 0, 5, 4, 0 }, { 0, 0, 40, 70, 4, 63, 0, 0, 60, 0, 4, 0 },
                        { 0, 32, 0, 0, 0, 5, 0, 3, 0, 660, 0, 9 } };

                Constrainer C = new Constrainer("");
                IntExpArray take = new IntExpArray(C, nbItems, 0, 6118, "take");
                IntExp costFunc = C.scalarProduct(take, value);
                for (int r = 0; r < nbResources; r++) {
                    C.postConstraint(C.scalarProduct(take, use[r]).le(capacity[r]));
                }

                Goal gm = null;
                switch (N) {
                    case 0:
                        gm = new GoalFastMinimize(new GoalGenerate(take), costFunc.neg(), false);
                        System.out.println(gn[0]);
                        break;
                    default:
                        gm = new GoalMinimize(new GoalGenerate(take), costFunc.neg(), false);
                        System.out.println(gn[N]);
                        break;
                }

                boolean flag = executeWithTimeout(C, gm, 3);

                // Goal solve = new GoalMinimize(new GoalGenerate(take),
                // costFunc.neg(), true);
                // boolean flag = C1.execute(solve);
                assertTrue("the solution hasn't been found", flag);
                assertTrue("cost function is still unbounded", costFunc.bound());
                assertEquals("The solution isn't an optimal one", 26152, costFunc.max());

            } catch (Failure f) {
                fail("test failed");
            }
        }

    } // end of testKnapsackProblem

    public void testSheduler1() {

        String r1;
        try {
            r1 = TwoOvens.main(1);
            System.out.println(r1);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("test");
        }
    }

    public void testStopExecute() {
        System.out.println("testStopExecute");
        try {
            Constrainer C = new Constrainer("TestConstraintExpEqualsExp");
            prepareChessProblem(C, 500);
            C.setTimeLimit(1);
            ChoicePointLabel lastLable = null;
            boolean flag = false;
            boolean firstTime = true;
            int timeLimsCounter = 0;
            while (true) {
                try {
                    if (firstTime) {
                        flag = C.execute(new GoalGenerate(x, new IntVarSelectorMinSizeMin(x), true), false);
                    } else {
                        flag = C.toContinue(lastLable, false);
                    }
                    break;
                } catch (TimeLimitException ex) {
                    timeLimsCounter++;
                    firstTime = false;

                    lastLable = ex.label();
                }
            }
            System.out.println(timeLimsCounter + ": time limit has been exceeded");
            assertTrue("the problem is inconsistent", flag);

            // check out the solution
            checkOutChessProblemSolution();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

}