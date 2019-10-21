// ------------------------------------------------------------
// File: TestExecution.java
// Java Implementation of the Dummy  problem
// Copyright (C) 2000 by Exigen Group
// ------------------------------------------------------------

package org.openl.ie.constrainer.test;

import org.openl.ie.constrainer.*;

public class TestBool {
    public static void main(String[] args) {
        TestBool benchmark = new TestBool();
        if (benchmark.run()) {
            System.out.println("Successful run of " + benchmark);
        } else {
            System.out.println("Unsuccessful run of " + benchmark);
        }
    } // ~ main

    public boolean run() {
        try {
            Constrainer c = new Constrainer("Test");

            IntVar var1 = c.addIntVar(1, 9, "var1");
            IntVar var2 = c.addIntVar(2, 10, "var2");
            IntVar var3 = c.addIntVar(0, 50, "var3");
            c.execute(var3.equals(var1.add(var2)));

            System.out.println(var1 + " " + var2 + " " + var3);

            IntVar x = c.addIntVar(-10, 10, "x");
            IntVar y = c.addIntVar(-10, 10, "y");
            IntVar z = c.addIntVar(-10, 10, "z");

            IntExpArray vars = new IntExpArray(c, x, y, z);

            IntExp sum = x.lt(y).and(y.gt(z)).add(x).add(y).add(z);
            sum.equals(3).post();

            Goal searchGoal = new GoalGenerate(vars);

            if (!c.execute(searchGoal)) {
                System.out.println("No solutions.");
            }

            System.out.println(vars);
            System.out.println("sum=" + sum + "[" + sum.min() + ".." + sum.max() + "]");

            return true;

        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            return false;
        }
    }

}
