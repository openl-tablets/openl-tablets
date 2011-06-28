package org.openl.ie.constrainer;

import org.openl.ie.constrainer.impl.ConstraintFloatExpLessValue;
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
 * constrained integer variable called the "cost".
 * <p>
 * This goal uses the search goal provided by the caller, and expects that this
 * goal instantiates the cost every time when a solution is found.
 *
 * To search for a solution which provides the minimal cost,
 * GoalFloatFastMinimize finds a solution, posts the constraint that the cost
 * variable should be less then the found cost and continues the search.
 * <p>
 * In contrast with GoalFloatMinimize, when a solution is found
 * GoalFloatFastMinimize does not restore all decision variables, but continues
 * the search just with a more strict constraint to the cost variable.
 *
 * When there are no solutions anymore, the latest found solution is the optimal
 * one.
 * <p>
 * By default, GoalFloatFastMinimize calculates the optimal solution twice.
 *
 * If the caller's search goal saves every found solution itself, the caller may
 * specify the mode "goal_saves_solution" to prevent the duplicate calculation
 * at the end of the search.
 *
 * @see GoalFloatMinimize
 */
public class GoalFloatFastMinimize extends GoalImpl {
    /**
     * This goal is executed after an optimal solution is found or no solution
     * exists. The constrainer state is as it was before
     * GoalFloatFastMinimize.execute().
     */
    class AnalyzeAndSet extends GoalImpl {
        AnalyzeAndSet(Constrainer C) {
            super(C, "AnalyzeAndSet");
        }

        public Goal execute() throws Failure {
            // check if the any solution was found
            if (_number_of_solutions <= 0) {
                constrainer().fail();
            }

            // Debug.on();Debug.print("Found solution with the cost =
            // "+_best_cost);Debug.off();

            if (_goal_saves_solution) {
                return null;
            }

            // post the constraint '_cost == _best_cost'
            double eps = 1e-6; // ???
            double search_cost_min = _best_cost_min - eps;
            double search_cost_max = _best_cost_max + eps;
            _cost.moreOrEqual(search_cost_min).post();
            _cost.lessOrEqual(search_cost_max).post();

            // find the optimal solution
            return _goal_find_solution;
        }
    }
    /**
     * Activates _constraint_limit_cost and organizes the optimization loop
     * using _goal_find_solution and GoalBacktrack.
     */
    class FindAndImprove extends GoalImpl {
        FindAndImprove(Constrainer C) {
            super(C, "FindAndImprove");
        }

        public Goal execute() throws Failure {
            // activate _constraint_limit_cost
            _constraint_limit_cost.resetValue(_cost.max());
            _constraint_limit_cost.post();

            return new GoalAnd(_goal_find_solution, new GoalBacktrack(constrainer()));
        }
    }
    /**
     * This goal backtracks while _cost >= _best_cost. <br>
     * Then it sets _best_cost as a new value in _constraint_limit_cost. In this
     * case the search continues with the new limit on the cost. <br>
     * This goal fails if backtrack can not satisfy _cost < _best_cost. This
     * means that the is no better solution. In this case GoalAnalyzeAndSet will
     * be executed as an alternate goal for the "_rootLabel" choice point.
     */
    class GoalBacktrack extends GoalImpl {
        public GoalBacktrack(Constrainer c) {
            super(c, "GoalBacktrack");
        }

        public Goal execute() throws Failure {
            fixFoundSolution();

            // backtrack while _cost >= _best_cost
            ChoicePointLabel lbl = constrainer().currentChoicePointLabel();
            while (((lbl == null) || (!lbl.equals(_rootLabel))) && violated()) {
                if (!constrainer().backtrack()) {
                    throw new RuntimeException("Internal error in " + this);
                }

                if (_trace) {
                    Log.info(" Backtrack: cost" + _cost.domainToString() + " best_cost_min=" + _best_cost_min);
                }
            }

            // fail if still violated
            if (violated()) {
                constrainer().fail("GoalBacktrack");
            }

            // set _best_cost as a new limit in _constraint_limit_cost
            _constraint_limit_cost.resetValue(_best_cost_min - _cost_step);

            return null;
        }

        /**
         * Called when a solution (first or next) is found. Saves the current
         * value of the cost, increments _number_of_solutions, and performs
         * tracing.
         */
        void fixFoundSolution() {
            _best_cost_min = _cost.min();
            _best_cost_max = _cost.max();

            _number_of_solutions++;

            if (_trace) {
                Log.info("Solution " + _number_of_solutions + ": cost" + _cost.domainToString());
            }

            if (_client_tracer != null) {
                Log.info(_client_tracer.toString());
            }
        }

        /**
         * violated() for the constraint: _cost <= _best_cost - _cost_step
         */
        boolean violated() {
            return _cost.min() > _best_cost_min - _cost_step;
        }

    } // GoalBacktrack
    private FloatExp _cost;
    private double _best_cost_min;
    private double _best_cost_max;
    private Goal _goal_find_solution;
    private ConstraintFloatExpLessValue _constraint_limit_cost;
    private int _number_of_solutions;
    private boolean _trace;

    private Object _client_tracer;

    private boolean _goal_saves_solution;

    private ChoicePointLabel _rootLabel;

    private double _cost_step;

    /**
     * Constructor with a given generation goal, cost expression, trace flag,
     * and save_solution flag.
     *
     * Use "goal" to minimize "cost".
     *
     * If <code>("trace" = true)</code>, after each successful iteration,
     * solution# and the cost value will be printed.
     *
     * If <code>("goal_saves_solution" = true)</code>, the optimal solution
     * (if any) would not be calculated twice: it means the "goal" should save
     * the value of decision variables and the cost.
     */
    public GoalFloatFastMinimize(Goal goal, FloatExp cost, double costStep) {
        super(cost.constrainer(), "");
        _goal_find_solution = goal;
        _cost = cost;
        _constraint_limit_cost = new ConstraintFloatExpLessValue(_cost, _cost.max());
        _number_of_solutions = 0;

        _trace = false;
        _client_tracer = null;
        _goal_saves_solution = false;

        _rootLabel = constrainer().createChoicePointLabel();

        _cost_step = Math.abs(costStep);

    }

    public void clientTracer(Object tracer) {
        _client_tracer = tracer;
    }

    /**
     * The implementation of the search algorithm.
     * <p>
     * It sets labeled choice point from the goals FindAndImprove and
     * AnalyzeAndSet. <br>
     * FindAndImprove is a loop that finds an optimal solution (if any solution
     * exists). FindAndImprove always fails (the _cost can not be improved
     * infinitely). <br>
     * AnalyzeAndSet do further job:
     * <ul>
     * <li>Checks if any solution was found and fails if not.
     * <li>If _goal_save_solution is true then it finishes.
     * <li>Otherwise the constraint _cost==_best_cost is posted and
     * _goal_find_solution is executed to instantiate the optimal solution.
     * </li>
     * </ul>
     */
    public Goal execute() throws Failure {
        return new GoalOr(new FindAndImprove(constrainer()), new AnalyzeAndSet(constrainer()), _rootLabel);
    }

    public void savesSolutionFlag(boolean flag) {
        _goal_saves_solution = flag;
    };

    /**
     * Returns a String representation of this goal.
     */
    @Override
    public String toString() {
        return "Use " + _goal_find_solution.name() + " to minimize(" + _cost.name() + ")";
    }

    public void traceFlag(boolean flag) {
        _trace = flag;
    }

} // ~GoalFloatFastMinimize
