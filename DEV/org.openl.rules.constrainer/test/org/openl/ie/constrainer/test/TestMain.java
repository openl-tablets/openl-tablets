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
import org.openl.ie.constrainer.Expression;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatExpConst;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAllSolutions;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFail;
import org.openl.ie.constrainer.GoalFloatGenerate;
import org.openl.ie.constrainer.GoalFloatInstantiate;
import org.openl.ie.constrainer.GoalFloatMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalMinimize;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntValueSelector;
import org.openl.ie.constrainer.IntValueSelectorMin;
import org.openl.ie.constrainer.IntValueSelectorMinMax;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.IntVarSelector;
import org.openl.ie.constrainer.IntVarSelectorMinSize;
import org.openl.ie.constrainer.impl.ConstraintAllDiff;
import org.openl.ie.constrainer.impl.FloatExpAddExp;
import org.openl.ie.constrainer.impl.GoalFastMinimizeOld;

/**
 * Various tests + main method to run them.
 */
public class TestMain {
    /*
     * ------------------------------------------------------------ ILOG
     * examples: equation.cpp Problem description ------------------ Solve the
     * equation: x + x^3 + exp(x) = 10 with x in [1,10]
     * ------------------------------------------------------------
     */
    /*
     * x[1.55113..1.55113]
     */
    static void equation_cpp() throws Exception {
        Constrainer c = new Constrainer("");
        c.printInformation();

        FloatVar x = c.addFloatVar(1., 10., "x");

        FloatExpArray vars = new FloatExpArray(c, x);

        // c.addConstraint("x + x**3 + x.exp() == 10");
        c.addConstraint(x.add(x.pow(3)).add(x.exp()).eq(10));

        c.postConstraints();
        Goal goal = new GoalFloatGenerate(vars);
        // goal = new GoalAnd(goal,new GoalPrintSolution(vars));
        c.execute(goal);
        System.out.println("Solution: " + vars);
    }

    /**
     * The main method to run various tests.
     */
    public static void main(String args[]) {
        long t1 = System.currentTimeMillis();

        try {
            equation_cpp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long t2 = System.currentTimeMillis();
        System.out.println("Execution time:" + (t2 - t1) / 1000. + "sec");
    }

    static void mainFromConstrainer(String args[]) {
        try {
            Constrainer C = new Constrainer("Constrainer Test");

            IntVar x = C.addIntVar(0, 10, "x", IntVar.DOMAIN_DEFAULT);
            IntVar y = C.addIntVar(0, 10, "y", IntVar.DOMAIN_DEFAULT);
            IntVar z = C.addIntVar(0, 1000, "z", IntVar.DOMAIN_DEFAULT);

            /*
             * (x.more(3)).execute(); System.out.println(x + " " + y + " " + " " +
             * z + " after x>3");
             *
             * Constraint c1 = x.less(y); //c1.name("x<y");
             *
             * System.out.println(x + " " + y + " " + " " + z + " before " +
             * c1);
             *
             * c1.execute();
             *
             * System.out.println(x + " " + y + " " + " " + z + " after " + c1);
             *
             * (y.less(9)).execute(); System.out.println(x + " " + y + " " + " " +
             * z + " after y<9");
             *
             * (y.lessOrEqual(7)).execute(); System.out.println(x + " " + y + " " + " " +
             * z + " after y<=7");
             *
             * IntVar sum = C.sum(x,y); //sum.name("x+y"); Constraint c2 =
             * z.equals(sum); //c2.name("z=x+y"); c2.execute();
             * System.out.println(x + " " + y + " " + " " + z + " after " + c2);
             */
            int size = 3;
            IntExpArray vars = new IntExpArray(C, size);
            vars.set(x, 0);
            vars.set(y, 1);
            vars.set(z, 2);

            // C.trace(x);

            IntExp card = C.cardinality(vars, 5);
            System.out.println(card);
            // int[] values = { 4,5,6,7 };
            // Vector cards = C.distribute(vars,values);

            // C.trace(vars);

            Constraint constraintAllDiff = new ConstraintAllDiff(vars);
            constraintAllDiff.post();

            IntValueSelector value_selector = new IntValueSelectorMinMax();
            IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
            Goal solution = new GoalAnd(card.equals(1), new GoalGenerate(vars, var_selector, value_selector));

            // C.traceExecution();
            // C.traceFailures();
            // C.execute(solution,3,true);

            // Goal all_solutions = new GoalAnd(solution,new GoalFail(C));
            // C.execute(all_solutions);

            //
            IntExp cost = z.mul(4).sub(y.mul(2));
            if (!C.execute(new GoalMinimize(solution, cost))) {
                System.out.println("Can not minimize cost " + cost);
            }
            System.out.println(x + " " + y + " " + " " + z + " cost=" + cost);
            //
        } catch (Exception e) {
            System.out.println("Exception 2:  " + e);
            e.printStackTrace();
        }

        // Constrainer.end();
    }

    /**
     * Test of the expression abs(IntExp).
     */
    static void testAbs(String args[]) throws Exception {
        Constrainer c = new Constrainer("");

        IntVar x = c.addIntVar(-10, 10, "msft", IntVar.DOMAIN_BIT_FAST);
        IntVar y = c.addIntVar(-10, 10, "intc", IntVar.DOMAIN_BIT_FAST);
        IntVar z = c.addIntVar(-10, 10, "z", IntVar.DOMAIN_BIT_FAST);

        IntExp sum = new IntExpArray(c, x.abs(), y.abs()).sum();

        sum.less(8).post();

        IntExpArray vars = new IntExpArray(c, x, y);

        c.traceChoicePoints(vars);
        c.traceFailures(1, vars);
        c.traceFailureStack();
        // c.traceExecution();

        IntValueSelector value_selector = new IntValueSelectorMin();
        IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
        Goal searchGoal = new GoalGenerate(vars, var_selector, value_selector);

        Goal fail = new GoalFail(c);

        c.printInformation();

        // Goal goal = new GoalMinimize(searchGoal, exp);

        c.execute(searchGoal);

        System.out.println(vars);
    }

    /**
     * Test of the ConstraintAllDiff.
     */
    static void testConstraintAllDiff(String args[]) throws Exception {
        Constrainer C = new Constrainer("Test");

        IntExp x = C.addIntVar(0, 10, "x", IntVar.DOMAIN_DEFAULT);
        IntExp y = C.addIntVar(2, 10, "y", IntVar.DOMAIN_DEFAULT);
        IntExp z = C.addIntVar(0, 10, "z", IntVar.DOMAIN_DEFAULT);

        int size = 3;
        IntExpArray vars = new IntExpArray(C, size);
        vars.set(x, 0);
        vars.set(y, 1);
        vars.set(z, 2);

        Constraint constraintAllDiff = new ConstraintAllDiff(vars);
        constraintAllDiff.post();

        // IntExp cost = C.addIntVar(0,20,"cost");
        // Constraint sum = new ConstraintAddVector(vars,cost);
        // sum.post();
        IntExp cost = C.sum(vars);
        cost.name("cost");

        // x.lessOrEqual(y).post();
        x.mul(2).sub(cost).more(y).post();

        C.traceChoicePoints(vars);
        // C.displayOnBacktrack(vars);
        C.traceFailures(vars);

        IntValueSelector value_selector = new IntValueSelectorMin();
        IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
        Goal solution =  new GoalGenerate(vars, var_selector, value_selector);

        // if (!C.execute(new GoalMinimize(solution,cost)))
        if (!C.execute(solution)) {
            System.out.println("No solutions");
        }
        System.out.println(x + " " + y + " " + " " + z + cost);
    }

    /**
     * Test of the expression (value ** FloatExp).
     */
    static void testExponent(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        IntVar x = c.addIntVar(1, 10, "x");
        IntVar y = c.addIntVar(1, 10, "y");

        IntExpArray vars = new IntExpArray(c, x, y);

        FloatExp xF = x.asFloat();
        FloatExp yF = y.asFloat();

        double base = 5;

        FloatExp exp1 = xF.exp(base);
        double v = Math.pow(base, 9);
        // xF.lessOrEqual(yF).post();
        exp1.moreOrEqual(v - 1e-1).post();
        exp1.lessOrEqual(v + 1e-1).post();
        System.out.println(xF + " " + yF);

        Goal searchGoal = new GoalGenerate(vars);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(vars);
        System.out.println("exp1=" + exp1.domainToString() + " == " + v);
    }

    /**
     * Test for GoalAllSolutions.
     */
    static void testGoalAllSolutions(String args[]) {
        try {
            Constrainer C = new Constrainer("Constrainer Test");

            IntVar x = C.addIntVar(0, 10, "x");
            IntVar y = C.addIntVar(0, 10, "y");
            IntVar z = C.addIntVar(0, 1000, "z");

            x.more(3).post();

            x.less(y).post();

            z.equals(C.sum(x, y)).post();

            IntExpArray vars = new IntExpArray(C, x, y, z);

            IntValueSelector value_selector = new IntValueSelectorMinMax();
            IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
            Goal solution = new GoalGenerate(vars, var_selector, value_selector);

            Goal all_solutions = new GoalAllSolutions(solution);
            C.printInformation();
            C.execute(all_solutions);
        } catch (Exception e) {
            System.out.println("Exception:  " + e);
            e.printStackTrace();
        }
    }

    /**
     * Test of the GoalFastminimizeOld.
     */
    static void testGoalFastminimizeOld(String args[]) throws Exception {
        Constrainer C = new Constrainer("Constrainer Test");

        IntVar x = C.addIntVar(0, 10, "X");
        IntVar y = C.addIntVar(0, 10, "Y");
        IntVar z = C.addIntVar(0, 1000, "Z");

        x.more(3).post();

        x.less(y).post();

        z.equals(C.sum(x, y)).post();

        IntExpArray vars = new IntExpArray(C, x, y, z);

        final IntExp cost = x.mul(y).sub(z);
        cost.name(" Cost");
        IntVar cost_var = C.addIntVar(cost);

        IntValueSelector value_selector = new IntValueSelectorMinMax();
        IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
        Goal solution = new GoalGenerate(vars, var_selector, value_selector);

        boolean goal_saves_solution = true;
        Goal minimize = new GoalFastMinimizeOld(solution, cost, goal_saves_solution);
        // Goal minimize = new GoalFastMinimizeOld(solution,cost,tracer);
        // Goal minimize = new GoalMinimize(solution,cost);

        C.execute(minimize);
    }

    /**
     * Test of the GoalFloatMinimize.
     */
    static void testGoalFloatMinimize(String args[]) throws Exception {
        Constrainer C = new Constrainer("Constrainer Test");
        Constrainer.precision(0.01);
        System.out.println("===== Test float vars. Precision=" + Constrainer.precision());
        FloatVar fx = C.addFloatVar(0, 10, "fx");
        FloatVar fy = C.addFloatVar(0, 10, "fy");
        FloatVar fz = C.addFloatVar(0, 1000, "fz");

        fz.trace();

        (fx.moreOrEqual(3)).execute();
        System.out.println(fx + " " + fy + " " + " " + fz + " after fx>3");

        Constraint fc1 = fx.lessOrEqual(fy); // fc1.name("fx<fy");

        System.out.println(fx + " " + fy + " " + " " + fz + " before " + fc1);

        fc1.execute();
        System.out.println(fx + " " + fy + " " + " " + fz + " after " + fc1);

        (fy.lessOrEqual(9)).execute();
        System.out.println(fx + " " + fy + " " + " " + fz + " after fy<9");

        FloatExp fc2 = new FloatExpAddExp(fx, fy);
        fc2.equals(fz).post();
        System.out.println(fx + " " + fy + " " + " " + fz + " after " + fc2);

        /*
         * if (!C.execute(new GoalAnd(fx.instantiate(),fy.instantiate())))
         * System.out.println("Can not instantiate fx/fy");
         * System.out.println(fx + " " + fy + " " + " " + fz + " after
         * instantiate fx & fy");
         */
        // FloatVar fcost = fx.mul(1000.0).sub(fz);
        FloatExp fcost = fz.mul(4.).sub(fy.mul(2.));
        fcost.trace();
        Goal fsolution = new GoalAnd(new GoalFloatInstantiate(fx), new GoalFloatInstantiate(fy));
        if (!C.execute(new GoalFloatMinimize(fsolution, fcost))) {
            System.out.println("Can not minimize cost " + fcost);
        }
        System.out.println(fx + " " + fy + " " + " " + fz + " cost=" + fcost);
    }

    /**
     * The test for GoalMinimize. Here is the source code: <code>
     * <pre>
     *       :static void main(String args[])
     *       :{
     *       : try
     *       : {
     *       :   Constrainer C = new Constrainer(&quot;Constrainer Test&quot;);
     *       :
     *       :   IntVar x = C.addIntVar(0,10,&quot;X&quot;);
     *       :   IntVar y = C.addIntVar(0,10,&quot;Y&quot;);
     *       :   IntVar z = C.addIntVar(0,1000,&quot;Z&quot;);
     *       :
     *       :   x.more(3).post();
     *       :
     *       :   x.less(y).post();
     *       :
     *       :   z.equals(C.sum(x,y)).post();
     *       :
     *       :   IntExpArray vars = new IntExpArray(C, x,y,z);
     *       :
     *       :   final IntExp cost = x.mul(y).sub(z);
     *       :   cost.name(&quot; Cost&quot;);
     *       :   IntVar cost_var = C.addIntVar(cost);
     *       :
     *       :   IntValueSelector value_selector = new IntValueSelectorMinMax();
     *       :   IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
     *       :   Goal solution = new GoalAnd(new GoalGenerate(vars,var_selector,value_selector),
     *       :                               new GoalPrintSolution(vars),
     *       :                               new GoalPrintObject(C,cost_var));
     *       :
     *       :   boolean goal_saves_solution = true;
     *       :   Goal minimize = new GoalMinimize(solution,cost,goal_saves_solution);
     *       :
     *       :   C.execute(minimize);
     *       : }
     *       : catch(Exception e)
     *       : {
     *       :   System.out.println(&quot;Exception:  &quot; + e);
     *       :   e.printStackTrace();
     *       : }
     *       :}
     * </pre>
    </code>
     */
    static void testGoalMinimize(String args[]) throws Exception {
        Constrainer C = new Constrainer("Constrainer Test");

        IntVar x = C.addIntVar(0, 10, "X");
        IntVar y = C.addIntVar(0, 10, "Y");
        IntVar z = C.addIntVar(0, 1000, "Z");

        x.more(3).post();

        x.less(y).post();

        z.equals(C.sum(x, y)).post();

        IntExpArray vars = new IntExpArray(C, x, y, z);

        final IntExp cost = x.mul(y).sub(z);
        cost.name(" Cost");
        IntVar cost_var = C.addIntVar(cost);

        IntValueSelector value_selector = new IntValueSelectorMinMax();
        IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
        Goal solution = new GoalGenerate(vars, var_selector, value_selector);

        boolean goal_saves_solution = false;
        Goal minimize = new GoalMinimize(solution, cost, goal_saves_solution);
        // Goal minimize = new GoalMinimize(solution,cost);

        C.execute(minimize);
        System.out.println("\nAfter minimize:" + vars + cost_var);
    }

    /**
     * Test of the expression (1/FloatExp).
     */
    public static void testInverse(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        IntVar x = c.addIntVar(0, 10, "x", IntVar.DOMAIN_BIT_FAST);
        IntVar y = c.addIntVar(1, 10, "y", IntVar.DOMAIN_BIT_FAST);

        IntExpArray vars = new IntExpArray(c, x, y);

        FloatExp xF = x.asFloat();
        FloatExp yF = y.asFloat();

        FloatExp exp1 = xF.div(yF);
        Constraint c1 = exp1.moreOrEqual(7 / 8. - 1e-10);
        c1.post();
        Constraint c2 = exp1.lessOrEqual(7 / 8. + 1e-10);
        c2.post();

        System.out.println("c1=" + c1);
        System.out.println("c2=" + c2);
        System.out.println(xF + " " + yF);

        // c.traceChoicePoints(vars);
        // c.traceFailures(1, vars);
        // c.traceFailureStack();
        // c.traceExecution();

        Goal searchGoal = new GoalGenerate(vars);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(vars);
        System.out.println(xF + " " + yF);
        System.out.println(exp1 + exp1.domainToString());
    }

    /*
     * ------------------------------------------------------------ ~wilkins.cpp
     */

    /**
     * Test of the expression log(FloatExp).
     */
    public static void testLog(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        FloatVar xF = c.addFloatVar(.1, 100., "x");

        FloatExp exp1 = xF.log();
        double v = Math.log(77.199999);
        exp1.moreOrEqual(v - 1e-10).post();
        exp1.lessOrEqual(v + 1e-10).post();

        // c.traceChoicePoints(vars);
        // c.traceFailures(1, vars);
        // c.traceFailureStack();
        // c.traceExecution();

        Goal searchGoal = new GoalFloatInstantiate(xF);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(xF);
        System.out.println("exp1=" + exp1 + " == " + v);
    }

    /**
     * Test of the expression (IntExp*IntExp).
     */
    static void testMul(String[] args) throws Exception {
        Constrainer c = new Constrainer("");
        c.printInformation();

        // int trace = IntVarImplTrace.TRACE_ALL;
        int trace = 0;

        IntVar x = c.addIntVarTrace(-100, 100, "x", IntVar.DOMAIN_PLAIN, trace);
        IntVar y = c.addIntVarTrace(-100, 100, "y", IntVar.DOMAIN_PLAIN, trace);
        IntVar z = c.addIntVarTrace(-100, 100, "z", IntVar.DOMAIN_PLAIN, trace);

        IntExpArray vars = new IntExpArray(c, x, y, z);

        IntExp exp1 = x.mul(y);
        IntExp exp2 = exp1.mul(z);
        IntExp prod = exp2;
        prod.equals(7 * 6 * 8).post();
        Goal searchGoal = new GoalGenerate(vars);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(vars);
        System.out.println("prod=" + prod.min() + " " + prod.max());
    }

    /**
     * Test of the expression (FloatExp**value).
     */
    public static void testPower(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        FloatVar xF = c.addFloatVar(.1, 10., "x");

        FloatExp exp1 = xF.pow(xF.add(1.2));
        double v = Math.pow(4, 4 + 1.2);
        exp1.moreOrEqual(v - 1e-10).post();
        exp1.lessOrEqual(v + 1e-10).post();

        // c.traceChoicePoints(vars);
        // c.traceFailures(1, vars);
        // c.traceFailureStack();
        // c.traceExecution();

        Goal searchGoal = new GoalFloatInstantiate(xF);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(xF);
        System.out.println("exp1=" + exp1 + " == " + v);
    }

    /**
     * Test of the expression (FloatExp**intValue).
     */
    public static void testPowerInt(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        IntVar x = c.addIntVar(0, 10, "x", IntVar.DOMAIN_BIT_FAST);
        IntVar y = c.addIntVar(0, 10, "y", IntVar.DOMAIN_BIT_FAST);

        IntExpArray vars = new IntExpArray(c, x, y);

        FloatExp xF = x.asFloat();
        FloatExp yF = y.asFloat();

        FloatExp exp1 = xF.pow(3);
        double v = Math.pow(7., 3.);
        exp1.moreOrEqual(v - 1e-10).post();
        exp1.lessOrEqual(v + 1e-10).post();
        System.out.println(xF + " " + yF);

        // c.traceChoicePoints(vars);
        // c.traceFailures(1, vars);
        // c.traceFailureStack();
        // c.traceExecution();

        Goal searchGoal = new GoalGenerate(vars);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(vars);
        System.out.println(xF + " " + yF);
        System.out.println("exp1=" + exp1);
    }

    /**
     * Test of the expression rangeViolation(IntExp,min,max).
     */
    static void testRangeViolation(String args[]) throws Exception {
        Constrainer c = new Constrainer("");
        c.showInternalNames(true);

        // int trace = 0;
        int trace = Expression.TRACE_ALL;
        IntVar x = c.addIntVarTrace(-10, 10, "msft", IntVar.DOMAIN_PLAIN, trace);
        IntVar y = c.addIntVarTrace(10, 10, "intc", IntVar.DOMAIN_PLAIN, trace);
        IntVar z = c.addIntVarTrace(-10, 10, "yhoo", IntVar.DOMAIN_PLAIN, trace);
        IntExpArray vars = new IntExpArray(c, x, y);

        IntExp exp = x.add(y).rangeViolation(12, 13);
        // ConstraintExpLessValue ct =
        // (ConstraintExpLessValue)exp.lessOrEqual(0);
        // ct.post();

        System.out.println(vars);

        Goal searchGoal = new GoalGenerate(vars);

        // Goal goal = new GoalMinimize(searchGoal, exp);

        c.printInformation();
        c.traceExecution();
        // c.trace(vars);
        c.traceFailures();

        c.execute(searchGoal);

        System.out.println(vars);
        System.out.println(exp);
    }

    /**
     * Test of the expression sqr(FloatExp).
     */
    public static void testSqr(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        IntVar x = c.addIntVar(0, 10, "x", IntVar.DOMAIN_BIT_FAST);
        IntVar y = c.addIntVar(0, 10, "y", IntVar.DOMAIN_BIT_FAST);

        IntExpArray vars = new IntExpArray(c, x, y);

        FloatExp xF = x.asFloat();
        FloatExp yF = y.asFloat();

        FloatExp exp1 = xF.sqr().add(yF.sqr());
        double v = Math.pow(3., 2) + Math.pow(7, 2);
        exp1.moreOrEqual(v - 1e-10).post();
        exp1.lessOrEqual(v + 1e-10).post();
        System.out.println(xF + " " + yF);

        // c.traceChoicePoints(vars);
        // c.traceFailures(1, vars);
        // c.traceFailureStack();
        // c.traceExecution();

        Goal searchGoal = new GoalGenerate(vars);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(vars);
        System.out.println(xF + " " + yF);
        System.out.println("exp1=" + exp1.domainToString() + "==" + v);
    }

    static void wilkins_cpp(String[] args) throws Exception {
        double xmin = -1e10, xmax = 1e10;
        FloatExp x;
        while ((x = wilkins_cpp_oneSolution(xmin, xmax)) != null) {
            wilkins_cpp_printSolution(x.value());
            xmin = x.max() + Math.abs(x.max()) * 1e-1;
        }
        wilkins_cpp_printSolverSolutions();
    }

    /*
     * ------------------------------------------------------------ ILOG
     * examples: wilkins.cpp Problem Description ------------------- Solve the
     * equation: (x + 1)(x + 2)...(x + 20) + 2^(-23)x^19 = 0 with x in [-1e10,
     * 1e10] ------------------------------------------------------------
     */
    /*
     * Results displayed x[-1..-1] x[-2..-2] x[-3..-3] x[-4..-4]
     * x[-4.9999999..-4.9999999] x[-6.0000069..-6.0000069]
     * x[-6.9996972..-6.9996972] x[-8.0072676..-8.0072676]
     * x[-8.9172502..-8.9172502] x[-20.846908..-20.846908]
     */
    static FloatExp wilkins_cpp_oneSolution(double xmin, double xmax) throws Exception {
        Constrainer c = new Constrainer("");
        c.showInternalNames(true);
        Constrainer.precision(1e-10);
        // c.traceFailures();
        // c.setTimeLimit(50);

        // int trace = FloatVarImplTrace.TRACE_ALL;
        int trace = 0;
        FloatVar x = c.addFloatVarTrace(xmin, xmax, "x", trace);

        FloatExp y = new FloatExpConst(c, 1);
        for (int i = 1; i <= 20; i++) {
            y = y.mul(x.add(i));
            y.name("y" + i);
            // if(i==1 || i==2)
            // {
            // y.trace();
            // }
        }

        // y.trace();

        double pow1 = Math.pow(2, -23);
        FloatExp exp1 = x.pow(19).mul(pow1);
        FloatExp exp2 = y.add(exp1);
        Constraint ct1 = exp2.equals(0);
        // Constraint ct1 = exp2.lessOrEqual(0+1e-3);
        // Constraint ct2 = exp2.moreOrEqual(0-1e-3);
        System.out.println("before post: " + x + " y" + y.domainToString());
        ct1.post();
        // ct2.post();
        // System.out.println("after post: " + x + " y" + y.domainToString());

        // c.traceChoicePoints(x);
        // c.traceFailures(1, x);
        // c.traceFailureStack();
        // c.traceExecution();

        Goal searchGoal = new GoalFloatInstantiate(x);

        if (!c.execute(searchGoal)) {
            System.out.println("No solutions!!!");
            return null;
        } else {
            System.out.println("Solution: " + x);
            System.out.println(" y=" + y.domainToString());
            System.out.println(" exp1=" + exp1.domainToString());
            System.out.println(" exp2=" + exp2.domainToString() + "==" + 0);
            return x;
        }
    }

    static void wilkins_cpp_printSolution(double _x) {
        double pow1 = Math.pow(2, -23);
        double _y = 1;
        for (int i = 1; i <= 20; i++) {
            _y = _y * (_x + i);
        }
        double _powExp = Math.pow(_x, 19) * pow1;
        double _exp2 = _y + _powExp;
        System.out.println(" _x=" + _x + " _powExp=" + _powExp + " _y=" + _y + " _exp2=" + _exp2);
    }

    static void wilkins_cpp_printSolverSolutions() {
        double[] x = { -1,// x[-1..-1]
                -2,// x[-2..-2]
                -2.9999999999998055,// x[-3..-3]
                -4.0000000002610232,// x[-4..-4]
                -4.9999999275515385,// x[-4.9999999..-4.9999999]
                -6.0000069439523003,// x[-6.0000069..-6.0000069]
                -6.9996972339360077,// x[-6.9996972..-6.9996972]
                -8.0072676034504493,// x[-8.0072676..-8.0072676]
                -8.9172502485170693,// x[-8.9172502..-8.9172502]
                -20.84690811158568,// x[-20.846908..-20.846908]
        };
        for (int i = 0; i < x.length; i++) {
            wilkins_cpp_printSolution(x[i]);
        }
    }

} // ~TestMain

