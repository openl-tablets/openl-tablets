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
 * constrained integer expression called the "cost".
 * <p>
 * This goal uses the search goal provided by the caller, and expects that this
 * goal instantiates the cost every time when a solution is found. GoalMinimize
 * uses the "branch and bound" technique to search for a solution which provides
 * the minimal cost. It recursively splits the domain of the cost variable into
 * two parts. The "precision" specifies when the search should stop. When the
 * difference between the cost max and min is less the precision, the currently
 * found solution is considered as the optimal one. Otherwise, the goal replaces
 * the cost domain by one of its halves, restores all decision variables and
 * calls itself again. By default, GoalMinimize calculates the optimal solution
 * twice. If the caller's search goal saves every found solution itself, the
 * caller may specify the mode "goal_saves_solution" to prevent the duplicate
 * calculation at the end of the search.
 *
 * @see GoalFastMinimize
 */
public class GoalMinimize extends GoalImpl {
    class SetCostMax extends GoalImpl {
        private IntExp _cost;

        SetCostMax(IntExp cost) {
            super(cost.constrainer());
            _cost = cost;
        }

        public Goal execute() throws Failure {
            bestCostMax(_cost.value());
            return null;
        }
    }
    private IntExp _cost;
    private Goal _find_solution;
    private int _best_cost_min;
    private int _best_cost_max;
    private int _precision;
    private int _number_of_choice_points;
    private int _number_of_failures;
    private int _number_of_solutions;
    private int _number_of_attempts;

    private boolean _goal_saves_solution;;

    private Goal _goal_set_cost_max;

    /**
     * The constructor with precision = 0 and goal_saves_solution = false.
     */
    public GoalMinimize(Goal goal, IntExp cost) {
        this(goal, cost, 0, false);
    }

    /**
     * The constructor with precision = 0.
     */
    public GoalMinimize(Goal goal, IntExp cost, boolean goal_saves_solution) {
        this(goal, cost, 0, goal_saves_solution);
    }

    /**
     * The constructor with goal_saves_solution = false.
     */
    public GoalMinimize(Goal goal, IntExp cost, int precision) {
        this(goal, cost, precision, false);
    }

    /**
     * This goal will use the search "goal" to find an optimal solution which
     * minimizes the "cost". The "precision" specifies when the search should
     * stop. When the difference between the cost max and min is less then the
     * precision, the currently found solution is considered as the optimal one.
     * If "goal_saves_solution" = true, the optimal solution (if any) would not
     * be calculated twice: it means the "goal" should save the value of
     * decision variables and the cost.
     */
    public GoalMinimize(Goal goal, IntExp cost, int precision, boolean goal_saves_solution) {
        super(cost.constrainer(), "");// "Use "+goal.name()+ " to
                                        // minimize("+cost.name()+")");
        _find_solution = goal;
        _cost = cost;
        _best_cost_max = _cost.max();
        _best_cost_min = _cost.min();
        _precision = precision;
        _number_of_choice_points = 0;
        _number_of_failures = 0;
        _number_of_solutions = 0;
        _number_of_attempts = 0;
        _goal_set_cost_max = new SetCostMax(_cost);
        _goal_saves_solution = goal_saves_solution;
    }

    private int bestCostMax() {
        return _best_cost_max;
    }

    private void bestCostMax(int max) {
        _best_cost_max = max;
    }

    private int bestCostMin() {
        return _best_cost_min;
    }

    private void bestCostMin(int min) {
        _best_cost_min = min;
    }

    /**
     * @return the cost variable
     */
    public IntExp cost() {
        return _cost;
    }

    /**
     * Implements the search algorithm.Uses the "branch and bound" technique to
     * search for a solution which provides the minimal cost. It recursively
     * splits the domain of the cost variable into two parts. Replaces the cost
     * domain by one of its halves, restores all decision variables and calls
     * itself again. When the difference between the cost max and min is less
     * the precision, the currently found solution is considered as the optimal
     * one.
     */
    public Goal execute() throws Failure {
        // Debug.on();Debug.print("Dichotomize with
        // cost["+bestCostMin()+";"+bestCostMax()+"]");Debug.off();
        double new_cost = ((double) (bestCostMin() + bestCostMax())) / 2;
        int best_cost = (int) Math.floor(new_cost);
        // if (best_cost - bestCostMin() <= _precision)
        if (bestCostMax() - bestCostMin() <= _precision) {
            if (_number_of_solutions > 0 || _number_of_attempts == 0) {
                constrainer().numberOfChoicePoints(_number_of_choice_points);
                constrainer().numberOfFailures(_number_of_failures);
                if (_number_of_solutions > 0) {
                    Log.info("Minimize Succeeded with cost[" + bestCostMin() + ";" + bestCostMax() + "]"
                            + ". Total Choice Points=" + _number_of_choice_points + " Failures=" + _number_of_failures
                            + " Number of solutions=" + _number_of_solutions);
                }
                if (_goal_saves_solution) {
                    return null;
                } else {
                    return new GoalAnd(setCost(), findSolution()); // success
                }
            } else {
                constrainer().fail("no solutions");
                return null;
            }
        }

        // dichotomized search
        boolean restore = true;
        _number_of_attempts++;
        // Debug.on();Debug.print(cost()+". Try cost
        // ["+bestCostMin()+";"+best_cost+"]");Debug.off();
        try {
            if (!constrainer().execute(
                    new GoalAnd(cost().moreOrEqual(bestCostMin()), cost().lessOrEqual(best_cost), findSolution(),
                            _goal_set_cost_max), restore)) {
                // Debug.on();Debug.print("Failure for cost <=
                // "+best_cost);Debug.off();
                bestCostMin(best_cost + 1);
            } else // success
            {
                _number_of_solutions++;
                // bestCostMax(best_cost);
                // Debug.on();Debug.print("Found solution with the cost <=
                // "+best_cost);Debug.off();
            }
        } catch (TimeLimitException ex) {
            constrainer().fail();
        }

        _number_of_choice_points += constrainer().numberOfChoicePoints();
        _number_of_failures += constrainer().numberOfFailures();
        return this; // continue
    }

    /**
     * @return the search goal
     */
    public Goal findSolution() {
        return _find_solution;
    }

    Goal setCost() {
        // Debug.on();Debug.print("Set Cost [ " + bestCostMin() + " ; " +
        // bestCostMax() + " ]");Debug.off();
        return new GoalAnd(_cost.moreOrEqual(bestCostMin()), _cost.lessOrEqual(bestCostMax()));
    }

    /**
     *
     * @throws Failure
     */
    @Override
    public boolean toContinue(ChoicePointLabel label, boolean restoration) {
        double new_cost = ((double) (bestCostMin() + bestCostMax())) / 2;
        int best_cost = (int) Math.floor(new_cost);
        if (!constrainer().toContinue(label, true)) {
            // Debug.on();Debug.print("Failure for cost <=
            // "+best_cost);Debug.off();
            bestCostMin(best_cost + 1);
        } else // success
        {
            _number_of_solutions++;
            // bestCostMax(best_cost);
            // Debug.on();Debug.print("Found solution with the cost <=
            // "+best_cost);Debug.off();
        }
        _number_of_choice_points += constrainer().numberOfChoicePoints();
        _number_of_failures += constrainer().numberOfFailures();
        return constrainer().execute(this);
    }

    /**
     * Returns a String representation of this goal.
     */
    @Override
    public String toString() {
        return "Use " + _find_solution.name() + " to minimize(" + _cost.name() + ")";
    }

} // ~GoalMinimize
