package org.openl.ie.constrainer.test;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalPrintSolution;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.GoalFastMinimizeOld;

/**
 * Incorrect implementation in the GoalFastMinimizeOld and correct one in the
 * GoalFastMinimize.
 *
 * Minimization problem depends on x only. GoalFastMinimizeOld continues the
 * search instantiating full domains of the other variables. GoalMinimize do
 * instantiation of other variables only one time.
 */
public class FastMinimizeOld {
    static Constrainer c;
    static IntExpArray vars;
    static IntExp cost;
    static Goal generate;

    static void fastMinimize() throws Exception {
        System.out.print("\n*** FastMinimize");
        problem();
        c.execute(new GoalFastMinimize(generate, cost));
    }

    static void fastMinimizeOld() throws Exception {
        System.out.print("\n*** FastMinimizeOld");
        problem();
        c.execute(new GoalFastMinimizeOld(generate, cost));
    }

    public static void main(String[] args) {
        try {
            fastMinimize();
            fastMinimizeOld();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void problem() throws Exception {
        c = new Constrainer("");

        int trace = 0;
        // int trace = IntVarImplTrace.TRACE_ALL;
        IntVar x = c.addIntVarTrace(0, 1, "x", IntVar.DOMAIN_BIT_FAST, trace);
        IntVar y = c.addIntVarTrace(0, 4, "y", IntVar.DOMAIN_BIT_FAST, trace);
        IntVar z = c.addIntVarTrace(0, 4, "z", IntVar.DOMAIN_BIT_FAST, trace);

        vars = new IntExpArray(c, x, y, z);
        vars.name("vars");

        cost = x.neg();
        // cost = x;

        generate = new GoalAnd(new GoalGenerate(vars), new GoalPrintSolution(vars));

        // c.traceFailures();
        // c.trace(vars);
        c.printInformation();
    }

} // ~FastMinimizeOld
