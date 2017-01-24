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
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.openl.ie.constrainer.ChoicePointLabel;
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FailureLimitException;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalMinimize;
import org.openl.ie.constrainer.GoalPrintSolution;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.IntVarSelectorMinSizeMin;
import org.openl.ie.constrainer.Session;
import org.openl.ie.constrainer.TimeLimitException;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class TestExecutionControl extends TestCase {
    private IntExpArray x, xplus, xminus;
    private final String _fileName = "c:\\session.ser";

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestExecutionControl.class));
        double d = Constrainer.FLOAT_MAX;
    }

    public TestExecutionControl(String name) {
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

    public void testCloseAndRestoreSession() {
        Constrainer C = new Constrainer("TestConstraintExpEqualsExp");
        ChoicePointLabel label = C.createChoicePointLabel();
        try {
            prepareChessProblem(C, 525);
        } catch (Failure f) {
            fail("Constrainer: internal error occured");
        }
        ;
        C.setTimeLimit(2);
        IntVar[] arr = new IntVar[x.size()];
        for (int i = 0; i < x.size(); i++) {
            arr[i] = (IntVar) x.get(i);
        }

        Session s = new Session(C, arr, null, new GoalGenerate(x, new IntVarSelectorMinSizeMin(x), true));
        boolean result;
        int tmlCounter = 1;

        while (true) {
            try {
                result = s.execute(false);
                break;
            } catch (TimeLimitException ex) {
                label = ex.label();
                System.out.println("TimeLimit#" + tmlCounter);
                tmlCounter++;
                try {
                    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("c:\\userInfo.ser"));
                    s.store(out);
                    out.flush();
                    out.close();

                    ObjectInputStream in = new ObjectInputStream(new FileInputStream("c:\\userInfo.ser"));
                    s = new Session(in);
                    in.close();
                } catch (java.lang.ClassNotFoundException cnfEx) {
                    cnfEx.printStackTrace();
                    fail("Class is not found!");
                } catch (java.io.IOException ioEx) {
                    ioEx.printStackTrace();
                    fail("IOException has occured");
                }
            }
        }

        assertTrue("The problem hasn't been solved!!!", result);
        IntVar[] queens = s.getIntVarsOfInterest();
        checkOutQueenProblemSolution(queens);
    }

    public void testMinimizeRestoration() {
        // knapsack problem
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
            for (int switcher = 0; switcher < 2; switcher++) {
                Constrainer C = new Constrainer("");
                IntExpArray take = new IntExpArray(C, nbItems, 0, 6118, "take");
                IntVar[] vars = new IntVar[take.size()];
                Object[] takeData = take.data();
                for (int i = 0; i < vars.length; i++) {
                    vars[i] = (IntVar) takeData[i];
                }
                IntExp costFunc = C.scalarProduct(take, value);
                Session s = null;

                if (switcher == 0) {
                    s = new Session(C, vars, null, new GoalMinimize(new GoalGenerate(take), costFunc.neg(), false));
                    s.getConstrainer().setTimeLimit(9);
                } else {
                    s = new Session(C, vars, null, new GoalFastMinimize(new GoalGenerate(take), costFunc.neg(), false));
                    s.getConstrainer().setTimeLimit(3);
                }
                for (int r = 0; r < nbResources; r++) {
                    C.postConstraint(C.scalarProduct(take, use[r]).le(capacity[r]));
                }
                boolean flag = false;
                int timeouts = 0;
                ChoicePointLabel lastLable = C.createChoicePointLabel();
                while (true) {
                    try {
                        flag = s.execute(false);
                        break;
                    } catch (TimeLimitException timelim) {
                        try {
                            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                                    new java.io.FileOutputStream(_fileName));
                            s.store(out);
                            out.flush();
                            out.close();

                            java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(
                                    _fileName));
                            s = new Session(in);

                        } catch (java.io.IOException ioEx) {
                            ioEx.printStackTrace();
                            fail("IOException occured!!!");
                        }

                        catch (java.lang.ClassNotFoundException cnfEx) {
                            cnfEx.printStackTrace();
                            fail("ClassNotFoundException occured!!!");
                        }
                        lastLable = timelim.label();
                        timeouts++;
                        System.out.println("Time limit exceedance#" + timeouts);
                    }
                }

                // Goal solve = new GoalMinimize(new GoalGenerate(take),
                // costFunc.neg(), true);
                // boolean flag = C1.execute(solve);
                assertTrue("the solution hasn't been found", flag);
                IntVar[] variables = s.getIntVarsOfInterest();
                int sum = 0;
                for (int i = 0; i < variables.length; i++) {
                    sum += variables[i].max() * value[i];
                }

                assertEquals("The solution isn't an optimal one", 26152, sum);
                if (switcher == 0) {
                    System.out.println("GoalMinimize succeeded");
                } else {
                    System.out.println("GoalFastMinimize succeeded");
                }
            }

        } catch (Failure f) {
            fail("test failed");
        }
    } // end of testKnapsackProblem

    public void testObtainingSuccessiveSolutions() {
        final Constrainer C = new Constrainer("TestConstraintExpEqualsExp");
        Object label = null;
        try {
            prepareChessProblem(C, 8);
        } catch (Failure f) {
            fail("Constrainer: internal error occured");
        }
        final java.util.ArrayList solutions = new java.util.ArrayList();

        // class being responsible for saving current solution into "solutions"
        class GoalSaveSolution extends GoalImpl {
            public GoalSaveSolution() {
                super(C);
            }

            public Goal execute() throws Failure {
                int[] solution = new int[x.size()];
                for (int i = 0; i < x.size(); i++) {
                    solution[i] = x.get(i).max();
                }
                solutions.add(solution);
                return null;
            }
        }

        class GoalFailureAndExit extends GoalImpl {
            public GoalFailureAndExit(Constrainer C) {
                super(C);
            }

            public Goal execute() throws Failure {
                constrainer().setFailuresLimit(1);
                constrainer().fail();
                return null;
            }
        }

        class GoalGenerateSuccessive extends GoalImpl {
            public GoalGenerateSuccessive() {
                super(C);
            }

            public Goal execute() throws Failure {
                Goal g = new GoalAnd(new GoalGenerate(x, new IntVarSelectorMinSizeMin(x), null), new GoalPrintSolution(
                        x), new GoalFailureAndExit(C));
                return g;
            }
        }
        Goal g = new GoalGenerateSuccessive();
        boolean firstTime = true;

        int nbSolutions = 0;
        while (true) {
            try {
                if (firstTime) {
                    firstTime = false;
                    C.execute(g);
                } else {
                    System.out.print("\nnext solution:");
                }
                C.toContinue(null, false);
                break;
            } catch (FailureLimitException fEx) {
                C.setFailuresLimit(0);
            }
        }
    } // end of testObtainingSuccessiveSolutions

    public void testStopExecute() {
        try {
            Constrainer C = new Constrainer("TestStopExecution");
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
