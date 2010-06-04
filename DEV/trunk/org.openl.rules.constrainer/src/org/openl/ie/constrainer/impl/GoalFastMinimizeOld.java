package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFail;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalMinimize;
import org.openl.ie.constrainer.IntExp;

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
 * To search for a solution which provides the minimal cost, GoalFastMinimize
 * finds a solution, saves its cost, posts the constraint that the cost variable
 * should be less then the found cost and calls itself recursively.
 * <p>
 * In contrast with GoalMinimize, when a solution is found GoalFastMinimize does
 * not restore all decision variables, but continues the search just with a more
 * strict constraint to the cost variable.
 *
 * When there are no solutions anymore, the latest found solution is the optimal
 * one.
 * <p>
 * By default, GoalFastMinimize calculates the optimal solution twice.
 *
 * If the caller's search goal saves every found solution itself, the caller may
 * specify the mode "goal_saves_solution" to prevent the duplicate calculation
 * at the end of the search.
 *
 * @see GoalMinimize
 */
public class GoalFastMinimizeOld extends GoalImpl {
    class DecrementCost extends GoalImpl {
        DecrementCost(Constrainer C) {
            super(C, "DecrementCost");
        }

        public Goal execute() throws Failure {
            _goal_limit_cost.resetValue(_cost.value());
            return null;
        }
    }
    class SaveCost extends GoalImpl {
        SaveCost(Constrainer C) {
            super(C, "SaveCost");
        }

        public Goal execute() throws Failure {
            _saved_cost = _cost.value();
            _number_of_solutions++;
            if (_trace) {
                System.out.println("\nSolution " + _number_of_solutions + ": cost=" + _cost.value());
            }
            if (_client_tracer != null) {
                System.out.println(_client_tracer.toString());
            }
            return null;
        }
    }
    class SetSavedCost extends GoalImpl {
        SetSavedCost(Constrainer C) {
            super(C, "SetSavedCost");
        }

        public Goal execute() throws Failure {
            return _cost.equals(_saved_cost);
        }
    }
    private IntExp _cost;
    private Goal _goal_find_solution;
    private ConstraintExpLessValue _goal_limit_cost;
    private Goal _goal_decrement_cost;
    private Goal _goal_save_cost;
    private Goal _goal_fail;
    private int _saved_cost;
    private int _number_of_solutions;

    private boolean _trace;;

    private Object _client_tracer;;

    private boolean _goal_saves_solution;;

    /**
     * Constructor with a given generation goal, and cost expression. Other
     * settings: trace = false and goal_saves_solution = false.
     */
    public GoalFastMinimizeOld(Goal goal, IntExp cost) {
        this(goal, cost, false, false);
    }

    /**
     * Constructor with a given generation goal, cost expression, trace flag. No
     * tracing information will be printed.
     */
    public GoalFastMinimizeOld(Goal goal, IntExp cost, boolean goal_saves_solution) {
        this(goal, cost, false, goal_saves_solution);
    }

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
    public GoalFastMinimizeOld(Goal goal, IntExp cost, boolean trace, boolean goal_saves_solution) {
        super(cost.constrainer(), "");// "Use "+goal.name()+ " to
                                        // minimize("+cost.name()+")");
        _goal_find_solution = goal;
        _cost = cost;
        _goal_limit_cost = new ConstraintExpLessValue(_cost, _cost.max() + 1);
        _goal_save_cost = new SaveCost(constrainer());
        _goal_decrement_cost = new DecrementCost(constrainer());
        _goal_fail = new GoalFail(constrainer());
        _number_of_solutions = 0;
        _trace = trace;
        _client_tracer = null;
        _goal_saves_solution = goal_saves_solution;
    }

    /**
     * Constructor with a given generation goal, cost expression, and tracer
     * object. After each successful iteration, tracer.toString() will be
     * printed.
     */
    public GoalFastMinimizeOld(Goal goal, IntExp cost, Object tracer) {
        this(goal, cost, false);
        _client_tracer = tracer;
    }

    /**
     * Constructor with a given generation goal, cost expression, tracer object,
     * and save_solution flag.
     */
    public GoalFastMinimizeOld(Goal goal, IntExp cost, Object tracer, boolean goal_saves_solution) {
        this(goal, cost, false, goal_saves_solution);
        _client_tracer = tracer;
    }

    /**
     * The implementation of the search algorithm.
     * <p>
     * It uses the "branch and bound" technique to search for a solution which
     * provides the minimal cost. To search for a solution which provides the
     * minimal cost, GoalFastMinimize:
     * <ul>
     * <li>Finds a solution.
     * <li>Saves its cost.
     * <li>Posts the constraint that the cost variable should be less then the
     * found cost.
     * <li>Calls itself recursively.
     * </ul>
     * If fails, the latest found solution is considered as the optimal one.
     */
    public Goal execute() throws Failure {
        constrainer().execute(
                new GoalAnd(_goal_limit_cost, _goal_find_solution, _goal_save_cost, _goal_decrement_cost, _goal_fail));
        if (_number_of_solutions > 0) {
            // Debug.on();Debug.print("Found solution with the cost =
            // "+_saved_cost);Debug.off();
            if (_goal_saves_solution) {
                return null;
            } else {
                return new GoalAnd(new SetSavedCost(constrainer()), _goal_find_solution);
            }
        } else {
            constrainer().fail();
            return null;
        }
    }

    /**
     * Returns a String representation of this goal.
     *
     * @return a String representation of this goal.
     */
    @Override
    public String toString() {
        return "Use " + _goal_find_solution.name() + " to minimize(" + _cost.name() + ")";
    }

} // ~GoalFastMinimizeOld
