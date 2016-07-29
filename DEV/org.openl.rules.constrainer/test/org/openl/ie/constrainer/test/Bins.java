// ------------------------------------------------------------
// File: Bins.java
// Java Implementation of the bins/components configuration problem
// Copyright (C) 2001 by Exigen Group
// ------------------------------------------------------------
/*
 Problem Description from ILOG Solver User Manual

 Given a supply of different components and bins of given types,
 determine all assignments of components to bins satisfying
 specified assignment constraints subject to an optimization criterion.

 In the following example there are 5 types of components:
 glass, plastic, steel, wood, copper

 There are three types of bins:
 red, blue, green

 whose capacity constraints are:
 red   has capacity 3
 blue  has capacity 1
 green has capacity 4

 whose containment constraints are:
 red   can contain glass, wood, copper
 blue  can contain glass, steel, copper
 green can contain plastic, wood, copper

 and requirement constraints are (for all bin types):
 wood requires plastic

 Certain component types cannot coexist:
 glass  exclusive copper
 copper exclusive plastic

 and certain bin types have capacity constraint for certain components:
 red   contains at most 1 of wood
 green contains at most 2 of wood

 Here is the components demand:
 1 of glass
 2 of plastic
 1 of steel
 3 of wood
 2 of copper

 What is the minimum total number of bins required to contain the components?

 ------------------------------------------------------------ */
package org.openl.ie.constrainer.test;

import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalInstantiate;
import org.openl.ie.constrainer.IntArray;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;


public class Bins {
    // =========================================================== one Bin
    class Bin {
        public int id;
        public IntVar type;
        public IntExp capacity;
        public IntExpArray counts; // per component

        Bin(int binId) throws Failure {
            id = binId;
            System.out.println("Create Bin-" + id);
            type = C.addIntVar(0, binTypes.length - 1, "bin" + id + "Type");
            int demand_max = demand[0];
            for (int i = 1; i < demand.length; ++i) {
                if (demand_max < demand[i]) {
                    demand_max = demand[i];
                }
            }
            counts = new IntExpArray(C, components.length, 0, demand_max, "bin" + id + "Components");
            // C.trace(counts);

            System.out.println("Define capacity-type constraint");
            capacity = elementAt(capacities, type);

            // bin constraints
            System.out.println("Post capacity constraint");
            IntExp counts_sum = counts.sum();
            C.postConstraint(counts_sum.gt(0));
            C.postConstraint(counts_sum.le(capacity));
            // C.postConstraint(counts_sum.more(0));
            // C.postConstraint(counts_sum.lessOrEqual(capacity));

            System.out.println("Post containment constraints");

            // red contains at most 1 of wood
            // IlcIfThen(_type == red, _counts[wood] <= 1);
            C.postConstraint(type.eq(red).implies(counts.elementAt(wood).le(1)));

            // green contains at most 2 of wood
            // IlcIfThen(_type == green, _counts[wood] <= 2);
            C.postConstraint(type.eq(green).implies(counts.elementAt(wood).le(2)));

            // red can contain glass, wood, copper
            // IlcIfThen(_type == red, _counts[plastic] == 0 && _counts[steel]
            // == 0);
            C.postConstraint(type.eq(red).implies(counts.elementAt(plastic).eq(0).and(counts.elementAt(steel).eq(0))));

            // blue can contain glass, steel, copper
            // IlcIfThen(_type == blue, _counts[plastic] == 0 && _counts[wood]
            // == 0);
            C.postConstraint(type.eq(blue).implies(counts.elementAt(plastic).eq(0).and(counts.elementAt(wood).eq(0))));

            // green can contain plastic, wood, copper
            // IlcIfThen(_type == green, _counts[glass] == 0 && _counts[steel]
            // == 0)
            C.postConstraint(type.eq(green).implies(counts.elementAt(glass).eq(0).and(counts.elementAt(steel).eq(0))));

            // wood requires plastic
            // IlcIfThen(_counts[wood] > 0, _counts[plastic] > 0);
            C.postConstraint(counts.elementAt(wood).gt(0).implies(counts.elementAt(plastic).gt(0)));

            // glass exclusive copper
            // m.add(counts[glass] == 0 || counts[copper] == 0);
            C.postConstraint(counts.elementAt(glass).eq(0).or(counts.elementAt(copper).eq(0)));

            // copper exclusive plastic
            // m.add(counts[copper] == 0 || counts[plastic] == 0);
            C.postConstraint(counts.elementAt(copper).eq(0).or(counts.elementAt(plastic).eq(0)));
        }

        IntExp count(int component) {
            return counts.elementAt(component);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            try {
                buf.append("Bin#").append(id).append(" (").append(binTypes[type.value()]).append("):");

                for (int i = 0; i < components.length; ++i) {
                    int c = counts.elementAt(i).value();
                    if (c > 0) {
                        buf.append(" ").append(components[i]).append("=").append(c);
                    }
                }
            } catch (Failure f) {
                System.out.println(f);
                buf.append("Bin-").append(id).append(" is not instantiated");
            }
            return buf.toString();
        }

    } // end Bin

    // The choice for the elementAt implementation.
    static int elementAt_impl = 0;

    // Components
    static final int glass = 0;
    static final int plastic = 1;
    static final int steel = 2;
    static final int wood = 3;
    static final int copper = 4;
    static final String[] components = { "glass", "plastic", "steel", "wood", "copper" };
    // Demand
    static final int[] _demand = { 1, 2, 1, 3, 2 };
    static int[] demand; // demand * size

    // Bins
    static final int red = 0;
    static final int blue = 1;
    static final int green = 2;
    static final String[] binTypes = { "red", "blue", "green" };
    static final int[] binCapacities = { 3, 1, 4 };

    // Constrainer
    static Constrainer C = null;
    static IntArray capacities = null;

    // ================================================================== Bins
    private Vector _bins = null;

    private int numberOfBins = 0;
    static IntExp elementAt(IntArray ary, IntExp index) throws Failure {
        // SUM( (index==i)*ary[i] )
        if (elementAt_impl == 1) {
            IntExp elementAt = null;
            for (int i = 0; i < ary.size(); i++) {
                IntExp v = index.eq(i).mul(ary.elementAt(i));
                if (i == 0) {
                    elementAt = v;
                } else {
                    elementAt = elementAt.add(v);
                }
            }

            return elementAt;
        }
        // SUM( (index==i)*ary[i] ) as sum of the IntExpArray
        else if (elementAt_impl == 2) {
            IntExpArray sum = new IntExpArray(ary.constrainer(), ary.size());
            for (int i = 0; i < ary.size(); i++) {
                IntExp v = index.eq(i).mul(ary.elementAt(i));
                sum.set(v, i);
            }

            return sum.sum();
        }
        // elementAt expression
        else {
            return ary.elementAt(index);
        }

    }

    public static void main(String[] args) {
        int size = 1;
        long executionStart = System.currentTimeMillis();

        if (args.length > 0) {
            size = Integer.parseInt(args[0]);
        }

        // Create demand = _demand*size;
        demand = new int[_demand.length];
        for (int i = 0; i < _demand.length; ++i) {
            demand[i] = _demand[i] * size;
        }

        int numberOfBins = 1;
        while (true) {
            try {
                System.out.println("Solve the problem for " + numberOfBins + " bins");
                Bins bins = new Bins(numberOfBins);
                System.out.println("Search..");
                if (bins.solve()) {
                    long executionTime = System.currentTimeMillis() - executionStart;
                    System.out.println("Execution time: " + executionTime + " msec");
                    System.out.println("Solution: " + numberOfBins + " bins");
                    for (int b = 0; b < numberOfBins; b++) {
                        System.out.println(bins.bin(b));
                    }
                    break;
                }
            } catch (Failure f) {
                System.out.println(f);
            }
            System.out.println("No enough bins");
            numberOfBins++;
        }
    }

    public Bins(int n) throws Failure {
        numberOfBins = n;

        C = new Constrainer("Bins " + n);
        C.expressionFactory().useCache(true);
        C.printInformation();
        // C.showInternalNames(true);
        // C.traceFailures();

        capacities = new IntArray(C, binCapacities);

        System.out.println("Create " + numberOfBins + " bins..");
        _bins = new Vector(numberOfBins);
        for (int i = 0; i < numberOfBins; i++) {
            _bins.add(new Bin(i + 1));
            // avoid bins symmetry
            if (i > 0) {
                System.out.println("Avoid symmetry constraints for bin " + i);
                Bin bin1 = (Bin) _bins.elementAt(i - 1);
                Bin bin2 = (Bin) _bins.elementAt(i);
                C.postConstraint(bin1.type.le(bin2.type));
                // C.postConstraint(bin1.type.lessOrEqual(bin2.type));
            }
        }

        System.out.println("Post demand constraints..");
        for (int j = 0; j < components.length; j++) {
            IntExpArray jcounts = new IntExpArray(C, numberOfBins);
            jcounts.name("type" + j + "Components");
            for (int b = 0; b < numberOfBins; b++) {
                jcounts.set(bin(b).count(j), b);
            }
            C.postConstraint(jcounts.sum().eq(demand[j]));
            // C.postConstraint(jcounts.sum().equals(demand[j]));
        }
    }

    Bin bin(int i) {
        return (Bin) _bins.elementAt(i);
    }

    /**
     * This goal instantiates bin types for all bins and then counts for all
     * bins.
     */
    Goal searchGoal_1() {
        IntExpArray types = new IntExpArray(C, numberOfBins);
        IntExpArray counts = new IntExpArray(C, numberOfBins * components.length);
        int x = 0;
        for (int b = 0; b < numberOfBins; b++) {
            types.set(bin(b).type, b);
            for (int j = 0; j < components.length; j++) {
                counts.set(bin(b).count(j), x);
                x++;
            }
        }
        Goal instantiateBinTypes = new GoalGenerate(types);
        // C.trace(types);
        Goal instantiateBinComponents = new GoalGenerate(counts);
        Goal search = new GoalAnd(instantiateBinTypes, instantiateBinComponents);

        return search;
    }

    /**
     * This goal instantiates each bin sequentially: bin type and counts.
     */
    Goal searchGoal_2() {
        Goal search = null;
        for (int b = 0; b < numberOfBins; b++) {
            Goal instantiateBinType = new GoalInstantiate(bin(b).type);
            Goal instantiateBinComponents = new GoalGenerate(bin(b).counts);
            Goal g = new GoalAnd(instantiateBinType, instantiateBinComponents);
            if (search == null) {
                search = g;
            } else {
                search = new GoalAnd(search, g);
            }
        }

        return search;
    }

    boolean solve() {
        // C.traceFailures();

        Goal search = searchGoal_1();

        return C.execute(search);
    }
}
