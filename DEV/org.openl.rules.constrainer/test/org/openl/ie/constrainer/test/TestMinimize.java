package org.openl.ie.constrainer.test;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Expression;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

public class TestMinimize {

    public static void main(String[] args) {
        try {
            test2(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void test1(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        IntVar x = c.addIntVar(-10, 10, "msft", IntVar.DOMAIN_BIT_FAST);
        IntVar y = c.addIntVar(-10, 10, "intc", IntVar.DOMAIN_BIT_FAST);

        try {
            IntExp sum = new IntExpArray(c, x.abs(), y.abs()).sum();
            IntExpArray vars = new IntExpArray(c, x, y);

            sum.less(1).post();
            sum.more(0).post();

            Goal searchGoal = new GoalGenerate(vars);

            if (!c.execute(searchGoal)) {
                System.out.println("No solutions!!!");
            }

            System.out.println(vars);
            System.out.println(sum);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     *
     * public static void main(String[] args) {
     *
     *
     * Constrainer c = new Constrainer("");
     *
     * IntVar x = c.addIntVar(109, 201, "msft", IntVar.DOMAIN_PLAIN); IntVar y =
     * c.addIntVar(-110, 210, "intc", IntVar.DOMAIN_PLAIN); IntVar z =
     * c.addIntVar(-100, 100, "cmb", IntVar.DOMAIN_PLAIN);
     *
     * try { IntExp amt1 = x.mul(10);
     *
     * amt1.moreOrEqual(1100).post(); amt1.lessOrEqual(2000).post();
     *
     * IntExp amt2 = y.mul(10);
     *
     * amt2.moreOrEqual(-100).post(); amt2.lessOrEqual(200).post();
     *
     * IntExp amt3 = z.mul(5);
     *
     *
     *
     * IntExp val1 = amt1.mul(10); IntExp val2 = amt2.mul(20); IntExp val3 =
     * amt3.mul(30);
     *
     * IntExp exp_sum = new IntExpArray(c, val1, val2, val3).sum();
     * exp_sum.name("exp_sum = val1 + val2 + val3");
     *
     *
     * int value = 10000;
     *
     * IntExp exp1 = exp_sum.sub(value).mul(exp_sum.moreExp(value)).mul(2);
     *
     * IntExp abs1 = val1.abs(); IntExp abs2 = val2.abs(); IntExp abs3 =
     * val3.abs();
     *
     * IntExp exp2 = new IntExpArray(c, abs1, abs2, abs3).sum().mul(5);
     * exp2.name("exp2 = (abs1 + abs2 + abs3) * 5");
     *
     * final IntExp cost = new IntExpArray(c, exp1, exp2).sum(); cost.name("
     * Cost");
     *
     * cost.publish(EventOfInterest.VALUE); // IntVar cost_var =
     * c.addIntVar(cost); // IntExp exp = x.mul(y).mul(z);
     *
     * IntExpArray vars = new IntExpArray(c, x, y, z);
     *  // c.traceChoicePoints(vars); // c.traceFailures(1, vars); //
     * c.traceFailureStack(); // c.traceExecution();
     *  // IntValueSelector value_selector = new IntValueSelectorMin(); //
     * IntVarSelector var_selector = new IntVarSelectorMinSize(vars); // Goal
     * searchGoal = new GoalGenerate(vars,var_selector,value_selector);
     *
     * IntValueSelector value_selector = new IntValueSelectorMin();
     * IntVarSelector var_selector = new IntVarSelectorMinSize(vars);
     *
     *
     * class Tracer { int solution = 0; IntExp _cost; Tracer(IntExp mycost) {
     * _cost = mycost; } public String toString() { ++solution; try { return
     * "\nSolution: "+solution+", cost="+_cost.value() + ". "; } catch(Failure
     * f) { return "ERROR: unbound cost"; } } };
     *
     * Tracer tracer = new Tracer(cost);
     *
     *
     * Goal solution = new GoalAnd(new
     * GoalGenerate(vars,var_selector,value_selector), new
     * GoalPrintSolution(vars), new GoalPrintObject(c, tracer)); //new
     * GoalPrintObject(c, cost_var));
     *
     *
     *
     * Goal printSolution = new GoalPrint(vars, "Solution:");
     *
     * Goal fail = new GoalFail(c);
     *  // c.printInformation();
     *
     *
     * Goal goal = new GoalFastMinimize(solution, cost, false); // Goal goal =
     * new GoalMinimize(solution, cost);
     *
     * c.execute(goal);
     *
     * System.out.println(vars); } catch(Exception e) { e.printStackTrace(); }
     *  }
     *
     */

    static public void test2(String[] args) throws Exception {
        Constrainer c = new Constrainer("");

        // int trace = 0;
        int trace = Expression.TRACE_ALL;
        IntVar x = c.addIntVarTrace(-1, 3, "x", IntVar.DOMAIN_BIT_FAST, trace);
        IntVar y = c.addIntVarTrace(-10, 10, "y", IntVar.DOMAIN_BIT_FAST, trace);
        IntVar z = c.addIntVarTrace(-10000, 10000, "z", IntVar.DOMAIN_BIT_FAST, trace);
        IntExpArray vars = new IntExpArray(c, x, z);
        IntExp abs = x.neg();
        // IntExp abs = x.abs();
        final IntExp cost = abs.gt(0).mul(abs.lt(2)).mul(abs.neg().add(2));
        // IntExp exp2 = exp1.add(x);
        // IntExp exp3 = exp2.add(y);
        // IntExp sum = exp3.add(z);
        // sum.equals(3).post();

        c.traceFailures();
        c.traceExecution();
        cost.equals(z).post();

        // IntVar var = c.addIntVar(cost.min()-10, cost.max()+10);
        // cost.equals(var).post();

        Goal solution = new GoalGenerate(vars);

        // Goal searchGoal = new GoalGenerate(vars);
        Object tracer = new Object() {
            int solution = 0;

            @Override
            public String toString() {
                ++solution;
                try {
                    return "\nSolution " + solution + ": cost=" + cost.value();
                } catch (Failure f) {
                    return "ERROR: unbound cost";
                }
            }
        };

        boolean goal_saves_solution = true;
        Goal minimize = new GoalFastMinimize(solution, cost, tracer, goal_saves_solution);

        if (!c.execute(minimize)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(vars);

        // System.out.println("sum=" + sum + "[" + sum.min() + ".." + sum.max()
        // + "]");
        // System.out.println("exp1=" + exp1 + "[" + exp1.min() + ".." +
        // exp1.max() + "]");
        // System.out.println("exp2=" + exp2 + "[" + exp2.min() + ".." +
        // exp2.max() + "]");
        // System.out.println("exp3=" + exp3 + "[" + exp3.min() + ".." +
        // exp3.max() + "]");
    }

}
