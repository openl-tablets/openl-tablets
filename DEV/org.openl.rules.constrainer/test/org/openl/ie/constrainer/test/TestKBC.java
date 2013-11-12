package org.openl.ie.constrainer.test;

import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFloatFastMinimize;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalPrintObject;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntValueSelector;
import org.openl.ie.constrainer.IntValueSelectorMin;
import org.openl.ie.constrainer.IntVarSelector;
import org.openl.ie.constrainer.IntVarSelectorFirstUnbound;


public class TestKBC {
    // small-size input
    static final int numberOfBrokers = 3;
    static final double[] brokerFees = { 0.10, 0.11, 0.12 };
    static final int numberOfLevels = 2;
    static final double[] levelCosts = { 69.5, 69.8 };
    static final double costMin = 69.0;

    // static final int numberOfBrokers = 40;
    // static final double[] brokerFees =
    // { 0.10, 0.11, 0.12, 0.10, 0.11, 0.09, 0.13, 0.10, 0.11, 0.10,
    // 0.10, 0.11, 0.12, 0.10, 0.11, 0.09, 0.13, 0.10, 0.11, 0.10,
    // 0.10, 0.11, 0.12, 0.10, 0.11, 0.09, 0.13, 0.10, 0.11, 0.10,
    // 0.10, 0.11, 0.12, 0.10, 0.11, 0.09, 0.13, 0.10, 0.11, 0.10
    // };
    // static final int numberOfLevels = 5;
    // static final double[] levelCosts = { 69.5, 69.8, 70.0, 70.3, 70.4 };
    // static final double costMin = 69.0;

    // static final int tolerancePercent = 2; // % of the totalCost
    static final int tolerancePercent = 5; // % of the totalCost
    static final int toleranceMax = 1000;

    public static void main(String[] args) {
        try {
            test1(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void test1(String[] args) throws Exception {
        Constrainer c = new Constrainer("KBC");

        c.traceFailures(5000);

        String arg = (args.length == 0) ? "1000" : args[0];
        double totalCost = Integer.parseInt(arg); // total cost of the stock
                                                    // to be traded
        int qty = (int) Math.ceil(totalCost / costMin); // qty of the stock to
                                                        // be traded

        // All Distribution vars
        IntExpArray distributionVars = new IntExpArray(c, numberOfBrokers * numberOfLevels);

        // Arrays of broker's distribution vars (one array per level)
        Vector levelDistributionArrays = new Vector(numberOfLevels);
        for (int l = 0; l < numberOfLevels; ++l) {
            levelDistributionArrays.add(new IntExpArray(c, numberOfBrokers));
        }

        // Distribution vars (sums of all levels)
        IntExpArray brokerDistributionVars = new IntExpArray(c, numberOfBrokers);

        // define distribution vars
        int i = 0;
        for (int b = 0; b < numberOfBrokers; ++b) {
            IntExpArray levelDistributionVars = new IntExpArray(c, numberOfLevels, 0, qty,
                    "levelDistributionVars for broker " + b);
            brokerDistributionVars.set(levelDistributionVars.sum(), b);
            for (int l = 0; l < numberOfLevels; ++l) {
                IntExp distibution = levelDistributionVars.elementAt(l);
                distributionVars.set(distibution, i++);
                // fill out levelDistributionArray for level "l" and "broker" b
                IntExpArray levelDistributionArray = (IntExpArray) levelDistributionArrays.elementAt(l);
                levelDistributionArray.set(distibution, b);
            }
        }

        // define cost per levels variables
        final FloatExp totalBrokersFee = c.scalarProduct(brokerDistributionVars, brokerFees);
        totalBrokersFee.name("TotalBrokersFee");
        IntExpArray levelCostVars = new IntExpArray(c, numberOfLevels);
        for (int l = 0; l < numberOfLevels; ++l) {
            IntExpArray levelDistributionArray = (IntExpArray) levelDistributionArrays.elementAt(l);
            levelCostVars.set(levelDistributionArray.sum(), l);
        }

        // Post cost constraints
        FloatExp totalCostVar = c.scalarProduct(levelCostVars, levelCosts);
        double tolerance = (totalCost * tolerancePercent) / 100;
        if (tolerance > toleranceMax) {
            tolerance = toleranceMax;
        }

        // {
        // for(int l=0; l < numberOfLevels; ++l)
        // {
        // IntExpArray levelDistributionArray =
        // (IntExpArray)levelDistributionArrays.elementAt(l);
        // c.trace(levelDistributionArray);
        // }
        // c.trace(totalCostVar);
        // }

        c.postConstraint(totalCostVar.le(totalCost + tolerance));
        c.postConstraint(totalCostVar.ge(totalCost - tolerance));

        // c.traceFailures();
        // c.traceExecution();

        IntVarSelector varSelector = new IntVarSelectorFirstUnbound(distributionVars);
        // IntValueSelector valueSelector = new IntValueSelectorMax();
        IntValueSelector valueSelector = new IntValueSelectorMin();
        Goal goalGenerate = new GoalGenerate(distributionVars, varSelector, valueSelector);

        Goal solution = new GoalAnd(goalGenerate, new GoalPrintObject(c, totalCostVar), new GoalPrintObject(c, "\n"),
                new GoalPrintObject(c, totalBrokersFee), new GoalPrintObject(c, "\n"));

        Goal minimize = new GoalFloatFastMinimize(solution, totalBrokersFee, 0.01);

        // c.setTimeLimit(20);

        if (!c.execute(minimize)) {
            System.out.println("No solutions!!!");
        }

        System.out.println(distributionVars);

    }

}
