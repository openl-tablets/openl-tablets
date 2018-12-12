package org.openl.ie.constrainer.test;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFail;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalGenerateAll;
import org.openl.ie.constrainer.IntArray;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

/**
 * Tests for the elementAt - constraint expression.
 */
public class TestElement {
    /**
     * The main method to run various tests.
     */
    public static void main(String args[]) {
        long t1 = System.currentTimeMillis();

        try {
            test_elementAt1();
            // testElementConstraint();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long t2 = System.currentTimeMillis();
        System.out.println("Execution time:" + (t2 - t1) / 1000. + "sec");
    }

    /**
     * Simple test for the elementAt expression.
     *
     * Java Constrainer: Solution 1: x[0], y[0] Solution 2: x[1], y[0]
     *
     * ILOG Solver: IlcIntVarArrayI[x[0] y[0]] IlcIntVarArrayI[x[1] y[0]] // ???
     * IlcIntVarArrayI[x[1] y[1]]
     *
     */
    static void test_elementAt1() throws Exception {
        Constrainer c = new Constrainer("Test");
        c.showInternalNames(true);
        c.printInformation();

        int trace = 0;
        // int trace = IntVar.TRACE_ALL;

        IntExp x = c.addIntVarTrace(0, 10, "x", IntVar.DOMAIN_DEFAULT, trace);
        IntExp y = c.addIntVarTrace(0, 10, "y", IntVar.DOMAIN_DEFAULT, trace);

        IntExpArray vars = new IntExpArray(c, x, y);
        vars.name("vars");
        IntArray values = new IntArray(c, 1, 2);
        values.name("values");

        // c.postConstraint("values[x+y] == values[x]");
        c.postConstraint(values.elementAt(x.add(y)).eq(values.elementAt(x)));

        Goal g = new GoalGenerateAll(vars);
        c.execute(g);
    }

    /**
     * Complex constraint using elementAt-expression.
     *
     */
    static void test_elementAt2() throws Exception {
        Constrainer c = new Constrainer("Test");
        c.showInternalNames(true);
        c.printInformation();

        int trace = 0;
        // int trace = IntVar.TRACE_ALL;

        IntExp x = c.addIntVarTrace(0, 10, "x", IntVar.DOMAIN_DEFAULT, trace);
        IntExp y = c.addIntVarTrace(0, 10, "y", IntVar.DOMAIN_DEFAULT, trace);
        IntExp z = c.addIntVarTrace(0, 10, "z", IntVar.DOMAIN_DEFAULT, trace);

        IntExpArray vars = new IntExpArray(c, x, y, z);
        vars.name("vars");
        IntArray values = new IntArray(c, 1, 2, 3, 4, 5);
        values.name("values");

        // c.postConstraint("|x-y|==1 && values[ values[x+y<5] ] == z");
        c.postConstraint(x.sub(y).abs().eq(1).and(values.elementAt(values.elementAt(x.add(y).lt(5))).eq(z)));

        Goal g = new GoalGenerateAll(vars);
        c.execute(g);
    }

    /**
     * Test of the ElementConstraint. Generates AllDiff-indeses (channels) and
     * print elementAt-costs.
     */
    static void testElementConstraint() throws Exception {
        Constrainer C = new Constrainer("TestElementConstraint");
        C.showInternalNames(true);
        C.printInformation();

        int size = 4;

        IntExpArray channel_vars = new IntExpArray(C, size);
        channel_vars.name("channel_vars");
        for (int i = 0; i < size; i++) {
            String name = "channel" + i;
            int domain = IntVar.DOMAIN_DEFAULT;
            int trace = 0;
            // int trace = IntVar.TRACE_ALL;
            IntVar channel_var = C.addIntVarTrace(0, size - 1, name, domain, trace);
            channel_vars.set(channel_var, i);
        }

        Constraint constraintAllDiff = C.allDiff(channel_vars);
        constraintAllDiff.post();

        IntArray cost_values = new IntArray(C, 50, 75, 35, 75);
        cost_values.name("cost_values");
        IntExpArray cost_vars = new IntExpArray(C, size);
        cost_vars.name("cost_vars");
        for (int i = 0; i < size; i++) {
            IntExp cost_var = cost_values.elementAt(channel_vars.get(i));
            cost_var.name("cost" + i);
            cost_vars.set(cost_var, i);
        }

        // C.postConstraint("channel_vars[0] < channel_vars[2]");
        C.postConstraint(channel_vars.elementAt(0).lt(channel_vars.elementAt(2)));

        // C.traceChoicePoints(channel_vars);
        // C.displayOnBacktrack(channels);
        // C.traceFailures(channel_vars);

        Goal solution = new GoalAnd(new GoalGenerate(channel_vars), new GoalFail(C));
        C.execute(solution);
    }

} // ~TestElement

