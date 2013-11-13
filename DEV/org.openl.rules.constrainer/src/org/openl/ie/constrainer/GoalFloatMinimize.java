package org.openl.ie.constrainer;

import org.openl.util.Log;

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
/**
 * An implementation of a {@link Goal} that finds a solution which minimizes a
 * constrained floating-point expression called the "cost".
 * <p>
 * This goal uses the search goal provided by the caller, and expects that this
 * goal instantiates the cost every time when a solution is found.
 * <p>
 * GoalMinimize uses the "branch and bound" technique to search for a solution
 * which provides the minimal cost.
 *
 * It recursively splits the domain of the cost variable into two parts.
 *
 * The "precision" specifies when the search should stop.
 *
 * When the difference between the cost max and min is less the precision, the
 * currently found solution is considered as the optimal one. Otherwise, the
 * goal replaces the cost domain by one of its halves, restores all decision
 * variables and calls itself again.
 * <p>
 * By default, GoalMinimize calculates the optimal solution twice.
 *
 * If the caller's search goal saves every found solution itself, the caller may
 * specify the mode "goal_saves_solution" to prevent the duplicate calculation
 * at the end of the search.
 */
public class GoalFloatMinimize extends GoalImpl {
    // static final int LEFT = 0;
    // static final int RIGHT = 1;

    private FloatExp _cost;
    private Goal _find_solution;
    private double _best_cost_min;
    private double _best_cost_max;
    // private int _direction;
    private int _number_of_choice_points;
    private int _number_of_failures;

    public GoalFloatMinimize(Goal goal, FloatExp cost) {
        super(cost.constrainer(), "Use " + goal.name() + " to minimize(" + cost.name() + ")");
        _find_solution = goal;
        _cost = cost;
        _best_cost_max = _cost.max(); // cost.constrainer().FLOAT_MAX;
        _best_cost_min = _cost.min();
        // _direction = LEFT;
        _number_of_choice_points = 0;
        _number_of_failures = 0;
    }

    private double bestCostMax() {
        return _best_cost_max;
    }

    private void bestCostMax(double max) {
        _best_cost_max = max;
    }

    private double bestCostMin() {
        return _best_cost_min;
    }

    private void bestCostMin(double min) {
        _best_cost_min = min;
    }

    private Constrainer C() {
        return _cost.constrainer();
    }

    public FloatExp cost() {
        return _cost;
    }

    /*
     * public Goal execute() throws Failure { Debug.on();
     * Debug.print("dichotomize with bestCost[" + bestCostMin() + ";" +
     * bestCostMax() + "]"); Debug.off(); double best_cost =
     * (bestCostMin()+bestCostMax())/2; if (best_cost - bestCostMin() <=
     * _cost.precision()) { return new GoalAnd(setCost(),findSolution()); //
     * success }
     *  // dichotomized search boolean restore = true; if (_direction == LEFT) {
     * if (!C().execute(new
     * GoalAnd(cost().less(best_cost),findSolution()),restore)) {
     * Debug.on();Debug.print("failure for cost <= "+best_cost);Debug.off();
     * _direction = RIGHT; //bestCostMin(best_cost); } else {
     * bestCostMax(best_cost); Debug.on();Debug.print("LEFT:Found solution with
     * the cost = "+best_cost);Debug.off(); }
     *  } else // RIGHT { if (!C().execute(new
     * GoalAnd(cost().more(best_cost),findSolution()),restore)) {
     * Debug.on();Debug.print("failure for cost >= "+best_cost);Debug.off();
     * _direction = LEFT; //bestCostMax(best_cost); } else {
     * bestCostMin(best_cost); Debug.on();Debug.print("RIGHT:Found solution with
     * the cost = "+best_cost);Debug.off(); } } return this; // continue }
     */
    public Goal execute() throws Failure {
        Debug.on();
        Debug.print("Dichotomize with cost[" + bestCostMin() + ";" + bestCostMax() + "]");
        Debug.off();
        double best_cost = (bestCostMin() + bestCostMax()) / 2;
        // if (best_cost - bestCostMin() <= _cost.precision())
        if (bestCostMax() - bestCostMin() <= Constrainer.FLOAT_PRECISION) {
            C().numberOfChoicePoints(_number_of_choice_points);
            C().numberOfFailures(_number_of_failures);
            Log.info("Minimize Succeeded. Total Choice Points=" + _number_of_choice_points + " Failures="
                    + _number_of_failures);
            return new GoalAnd(setCost(), findSolution()); // success
        }

        // dichotomized search
        boolean restore = true;
        Debug.on();
        Debug.print(cost().domainToString() + ". Try cost>=" + bestCostMin() + " and <=" + best_cost);
        Debug.off();
        if (!C().execute(new GoalAnd(cost().moreOrEqual(bestCostMin()), cost().lessOrEqual(best_cost), findSolution()),
                restore)) {
            Debug.on();
            Debug.print("Failure for cost <= " + best_cost);
            Debug.off();
            bestCostMin(best_cost);
        } else // success
        {
            bestCostMax(best_cost);
            Debug.on();
            Debug.print("Found solution with the cost <= " + best_cost);
            Debug.off();
        }

        _number_of_choice_points += C().numberOfChoicePoints();
        _number_of_failures += C().numberOfFailures();
        return this; // continue
    }

    public Goal findSolution() {
        return _find_solution;
    }

    public Goal setCost() {
        Debug.on();
        Debug.print("Set Cost [ " + bestCostMin() + " ; " + bestCostMax() + " ]");
        Debug.off();
        return new GoalAnd(_cost.lessOrEqual(bestCostMax()), _cost.moreOrEqual(bestCostMin()));
    }

} // ~GoalFloatMinimize
