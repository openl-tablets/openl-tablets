package org.openl.ie.constrainer;

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
import java.io.PrintStream;
import java.io.Serializable;

import org.openl.ie.constrainer.impl.ExpressionFactoryImpl;
import org.openl.ie.constrainer.impl.FloatVarImpl;
import org.openl.ie.constrainer.impl.FloatVarImplTrace;
import org.openl.ie.constrainer.impl.GoalStack;
import org.openl.ie.constrainer.impl.IntBoolVarImpl;
import org.openl.ie.constrainer.impl.IntSetVarImpl;
import org.openl.ie.constrainer.impl.IntVarImpl;
import org.openl.ie.constrainer.impl.IntVarImplTrace;
import org.openl.ie.constrainer.impl.UndoFastVectorAdd;
import org.openl.ie.constrainer.impl.UndoStack;
import org.openl.ie.constrainer.impl.UndoableFloatImpl;
import org.openl.ie.constrainer.impl.UndoableIntImpl;
import org.openl.ie.constrainer.impl.UndoableOnceImpl;
import org.openl.ie.tools.FastQueue;
import org.openl.ie.tools.FastStack;
import org.openl.ie.tools.FastVector;
import org.openl.ie.tools.RTExceptionWrapper;

/**
 * An implementation of the Constrainer - a placeholder for all variables, constraints, and search goals of the problem.
 * <p>
 * The Constrainer is a Java package for modeling and solving different constraint satisfaction problems.
 *
 * A problem is represented in terms of the decision variables and constraints, which define relationships between these
 * variables.
 *
 * The decision variables could be represented in form of Java objects which may use the predefined constrained
 * variables such as IntVar.
 *
 * The constraints themselves are objects inherited from a generic class Constraint.
 *
 * A user can define new business constraints.
 *
 * <p>
 * To find the problem solutions, the search algorithms could be represented using objects called Goals as building
 * blocks. The Constrainer supports a reversible environment with multiple choice points: when constraints/goals fail,
 * the Constrainer automatically backtracks to a previous choice point (if any).
 * <p>
 * There are several basic entities in the Constrainer:
 * <ol>
 * <li>Class Constrainer - a placeholder for all variables, constraints, and search goals of the problem
 * <li>Interface Subject - a base-class for constrained variables. Contains the major methods to allow constraints
 * (observers) observe the modification of the variables (subjects).
 * <li>Interface IntVar - constrained integer variables, the most popular subclass of the class Subject
 * <li>Interface Goal - a base class for different search goals and constraints.
 * </ol>
 *
 * @see Goal
 * @see IntVar
 * @see FloatVar
 * @see Subject
 * @author (C)2000 Exigen Group (http://www.IntelEngine.com)
 */

/*
 * Implementation notes
 *
 * GOALS EXECUTION. There are two major stacks: execution stack "EXE" and alternative stack "ALT". At each choice point
 * we create a new reversibility stack "REV". EXE.push(goal); while(!EXE.empty()) { execute(EXE.pop()); Goal execution
 * could: - push new subgoal on EXE (GoalAnd) - push goals on ALT (GoalOr) - fail. When failed: - pop from EXE all goals
 * pushed on it after the last choice point (done via marker) - if ALT.empty, FAILURE! - EXE.push(ALT.pop()) } SUCCESS!
 *
 */

public final class Constrainer implements Serializable {

    static public double FLOAT_PRECISION = 1.0e-6;

    // PRIVATE MEMBERS
    private String _name;
    private int _labelsCounter;
    private FastVector _intvars;
    private FastVector _floatvars;
    private FastVector _intsetvars;

    private FastVector _constraints;

    private int _choice_point = 0;

    private GoalStack _goal_stack;

    private UndoStack _reversibility_stack;
    private int _number_of_choice_points = 0;
    private int _number_of_failures = 0;
    private int _number_of_undos = 0;
    private long _time_limit = 0; // in seconds (0-no limit)
    private long _failures_limit = 0; // in failures (0-no limit)
    private FastVector _choice_point_objects;
    private FastVector _failure_objects;
    private boolean _trace_failure_stack;

    private int _failure_display_frequency;
    private FastVector _backtrack_objects;

    private boolean _trace_goals;

    private boolean _show_internal_names;
    private boolean _show_variable_names;

    private long _initial_memory;
    private long _max_occupied_memory;
    private long _number_of_notifications;

    private boolean _print_information;
    private long _execution_time = 0;

    private FastQueue _propagation_queue;

    private ExpressionFactory _expressionFactory;

    private FastStack _active_undoable_once;

    transient private PrintStream _out = System.out;

    /*
     * ============================================================================== Misc: toString(), helpers, ...
     * ============================================================================
     */
    /**
     * This method aborts the program execution. It prints the "msg" and the stack trace. Used to display "impossible"
     * errors.
     *
     * @param msg Diagnostic message to print.
     */
    static public void abort(String msg) {
        abort(msg, new RuntimeException(msg));
    }

    static public void abort(String msg, Throwable t) {
        throw RTExceptionWrapper.wrap(msg, t);
    }

    /*
     * ============================================================================== EOF High-level Components
     * ============================================================================
     */

    /**
     * Returns the precision of the constrained floating-point variable calculations.
     *
     * @return the precision of the constrained floating-point variable calculations.
     */
    static public double precision() {
        return FLOAT_PRECISION;
    }

    /**
     * Sets the precision of the constrained floating-point variable calculations. The default value is 1E-06.
     *
     * @param prc The new precision to be set.
     */
    static public void precision(double prc) {
        FLOAT_PRECISION = prc;
    }

    /**
     * Helper to print the vector of obects.
     *
     */
    static void printObjects(PrintStream out, String prefix, FastVector objects) {
        int size = objects.size();
        Object[] data = objects.data();
        for (int i = 0; i < size; i++) {
            out.print(prefix);
            out.println(data[i]);
        }
    }

    /**
     * Constructs a new constrainer - the object that serves as a placeholder for all other constrained objects,
     * constraints, and goals. Each problem should define at least one Constrainer object. All other objects relate to
     * this object.
     *
     * @param s Constrainer's symbolic name
     */
    public Constrainer(String s) {
        _initial_memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        _max_occupied_memory = _initial_memory;
        _name = s;

        _active_undoable_once = new FastStack();

        _intvars = new FastVector();
        _floatvars = new FastVector();
        _intsetvars = new FastVector();
        _constraints = new FastVector();

        _reversibility_stack = new UndoStack();
        _goal_stack = new GoalStack(_reversibility_stack);

        _propagation_queue = new FastQueue();

        _show_internal_names = false;
        _show_variable_names = true;
        _choice_point = 0;
        _number_of_choice_points = 0;
        _number_of_failures = 0;
        _number_of_notifications = 0;
        _failure_display_frequency = 0;
        _number_of_undos = 0;
        _choice_point_objects = new FastVector();
        _failure_objects = new FastVector();
        _backtrack_objects = new FastVector();
        _trace_goals = false;

        // _failure = new Failure();
        _trace_failure_stack = false;

        _print_information = false;

        // _undo_subject_factory = new UndoSubjectFactory();

        _expressionFactory = new ExpressionFactoryImpl(this);

    }

    /**
     * Adds an internal constrained floating-point variable to the Constrainer.
     */
    FloatVar addFloatVarInternal(FloatVar var) {
        _floatvars.addElement(var);
        addUndo(UndoFastVectorAdd.getUndo(_floatvars));
        // addInternalObjectToSymbolicContext(var);
        return var;
    }

    /**
     * Adds an internal constrained float variable to the Constrainer, selectively allows trace. Used in expressions
     * that create internal variables for their own needs. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     */
    public FloatVar addFloatVarTraceInternal(double min, double max, String name, int trace) {
        FloatVar var = trace != 0 ? new FloatVarImplTrace(this, min, max, name, trace)
                                  : new FloatVarImpl(this, min, max, name);
        return addFloatVarInternal(var);
    }

    /**
     * Adds a constrained boolean variable to the Constrainer.
     *
     * @param var Variable to add.
     * @return Added variable.
     */
    IntBoolVar addIntBoolVar(IntBoolVar var) {
        _intvars.add(var);
        addUndo(UndoFastVectorAdd.getUndo(_intvars));
        // addObjectToSymbolicContext(var.name(),var);
        return var;
    }

    /**
     * Creates and adds a constrained boolean variable to the Constrainer.
     *
     * @param name Variable's symbolic name.
     *
     * @return The added variable.
     */
    public IntBoolVar addIntBoolVar(String name) {
        IntBoolVar var = new IntBoolVarImpl(this, name);
        return addIntBoolVar(var);
    }

    /**
     * Adds an internal constrained boolean variable to the Constrainer.
     */
    IntBoolVar addIntBoolVarInternal(IntBoolVar var) {
        _intvars.add(var);
        addUndo(UndoFastVectorAdd.getUndo(_intvars));
        // addInternalObjectToSymbolicContext(var);
        return var;
    }

    /**
     * Adds an internal constrained boolean variable to the Constrainer. Used in expressions that create internal
     * variables for their own needs. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     */
    public IntBoolVar addIntBoolVarInternal(String name) {
        IntBoolVar var = new IntBoolVarImpl(this, name);
        return addIntBoolVarInternal(var);
    }

    /**
     * Create new constrained set variable and adds it to the constrainer
     *
     * @param values set of possible values
     * @return variable created based on set of possible values
     */
    public IntSetVar addIntSetVar(int[] values) {
        IntSetVar var = new IntSetVarImpl(this, values, "");
        return addIntSetVar(var);
    }

    /*
     * ============================================================================== Variables
     * ============================================================================
     */
    /**
     * Adds a constrained set variable to the Constrainer
     *
     * @param var IntSetVar to be added
     * @return passed variable
     */
    IntSetVar addIntSetVar(IntSetVar var) {
        _intsetvars.addElement(var);
        addUndo(UndoFastVectorAdd.getUndo(_intsetvars));
        return var;
    }

    /**
     * Adds a constrained integer variable to the Constrainer.
     *
     * @param min The minimum possible value of the variable being added.
     * @param max The maximum possible value of the variable being added.
     * @return The added variable.
     */
    public IntVar addIntVar(int min, int max) {
        return addIntVar(min, max, "", IntVar.DOMAIN_DEFAULT);
    }

    /**
     * Adds a constrained integer variable to the Constrainer.
     *
     * @param min The minimum possible value of the variable being added.
     * @param max The maximum possible value of the variable being added.
     * @param type The {@link Domain} type of the variable being added.
     *
     * @return The added variable.
     */
    public IntVar addIntVar(int min, int max, int type) {
        return addIntVar(min, max, "", type);
    }

    /**
     * Adds a constrained integer variable to the Constrainer.
     *
     * @param min The minimum possible value of the variable being added.
     * @param max The maximum possible value of the variable being added.
     * @param name Variable's symbolic name.
     * @return The added variable.
     */
    public IntVar addIntVar(int min, int max, String name) {
        return addIntVar(min, max, name, IntVar.DOMAIN_DEFAULT);
    }

    /**
     * Adds a constrained integer variable to the Constrainer.
     *
     * @param min The minimum possible value of the variable being added.
     * @param max The maximum possible value of the variable being added.
     * @param name Variable's symbolic name.
     * @param type The {@link Domain} type of the variable being added.
     * @return The added variable.
     */
    public IntVar addIntVar(int min, int max, String name, int type) {
        IntVar var = new IntVarImpl(this, min, max, name, type);
        return addIntVar(var);
    }

    /**
     * Adds a constrained integer variable to the Constrainer.
     *
     * @param var Variable to add.
     * @return Passed variable.
     */
    IntVar addIntVar(IntVar var) {
        _intvars.addElement(var);
        addUndo(UndoFastVectorAdd.getUndo(_intvars));
        // addObjectToSymbolicContext(var.name(),var);
        return var;
    }

    /**
     * Adds an internal constrained integer variable to the Constrainer.
     */
    IntVar addIntVarInternal(IntVar var) {
        _intvars.addElement(var);
        addUndo(UndoFastVectorAdd.getUndo(_intvars));
        // addInternalObjectToSymbolicContext(var);
        return var;
    }

    /**
     * Adds an internal constrained integer variable to the Constrainer, selectively allows trace. Used in expressions
     * that create internal variables for their own needs. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     */
    public IntVar addIntVarTraceInternal(int min, int max, String name, int type, int trace) {
        IntVar var = trace != 0 ? new IntVarImplTrace(this, min, max, name, type, trace)
                                : new IntVarImpl(this, min, max, name, type);
        return addIntVarInternal(var);
    }

    /*
     * ============================================================================== Propagation
     * ============================================================================
     */
    /**
     * Adds the subject (usually variable) to the propagation queue. It happenes when the subject changes its state. The
     * notificatioin events will be generated in {@link #propagate} method.
     */
    public void addToPropagationQueue(Subject subject) {
        _propagation_queue.push(subject);
    }

    /*
     * ============================================================================== Undo objects
     * ============================================================================
     */
    /**
     * Adds an undo-object to the reversibility stack.
     *
     * @param undo_object Undo object to add.
     */
    public void addUndo(Undo undo_object) {
        _number_of_undos++;
        // Debug.on();Debug.print("add " + undo_object);Debug.off();
        _reversibility_stack.pushUndo(undo_object);
    }

    /**
     * Adds an undo-object to the reversibility stack for a given undoable object. Some undo-objects can be generated
     * one time between choice points. Constrainer notifies such objects when backtrack or choice point occures.
     *
     * @param undo_object Undo object to add.
     * @param undoable Undoable object to add for notification.
     */
    public void addUndo(Undo undo_object, Undoable undoable) {
        addUndo(undo_object);
        // Adds an undoableOnce to the _active_undoable_once.
        // Used in UndoableOnceImpl and allowUndos().
        if (undoable instanceof UndoableOnceImpl) {
            _active_undoable_once.push(undoable);
        }
    }

    /*
     * ============================================================================== EOF Variables
     * ============================================================================
     */

    /**
     * Adds an undoable float to the Constrainer.
     *
     * @param value Initial value.
     * @return Added undoable float.
     */
    public UndoableFloat addUndoableFloat(double value) {
        return new UndoableFloatImpl(this, value);
    }

    /**
     * Adds an undoable integer to the Constrainer.
     *
     * @param value Initial value.
     * @return Added undoable integer.
     */
    public UndoableInt addUndoableInt(int value) {
        return new UndoableIntImpl(this, value);
    }

    /**
     * Clears the undone-flags for active undoable once objects. This force them to create undos again. Used: - when a
     * choice point is set - when backtracking is performed
     */
    void allowUndos() {
        while (!_active_undoable_once.empty()) {
            ((UndoableOnceImpl) _active_undoable_once.pop()).restore();
        }
    }

    /*
     * ============================================================================== Execution, backtracking, choice
     * points ============================================================================
     */
    /**
     * Backtracks to the most recent choice point.
     */
    boolean backtrack() {
        return backtrack(null);
    }

    /**
     * Backtracks to the most recent labeled choice point.
     */
    boolean backtrack(ChoicePointLabel label) {
        boolean success = _goal_stack.backtrack(label);

        allowUndos();

        if (success) {
            if (_backtrack_objects.size() > 0) {
                printObjects(_out, "BACKTRACK: ", _backtrack_objects);
            }
        }

        return success;
    }

    public int getStackSize() {
        return _reversibility_stack.size();
    }

    public void backtrackStack(int newSize) {
        _reversibility_stack.backtrack(newSize);
    }

    /**
     * Clears the propagation queue.
     */
    void clearPropagationQueue() {
        while (!_propagation_queue.empty()) {
            Subject var = (Subject) _propagation_queue.pop();
            var.inProcess(false);
            // var.clearPropagationEvents();
        }
    }

    /**
     * Returns a vector with all currently available constraints.
     *
     * @return Vector of added constraints.
     */
    public FastVector constraints() {
        return _constraints;
    }

    /**
     * generate a unique ChoicePointLabel
     *
     */
    public ChoicePointLabel createChoicePointLabel() {
        _labelsCounter++;
        return new ChoicePointLabel(this, _labelsCounter);
    }

    /**
     * Returns the label of the current choice point.
     */
    ChoicePointLabel currentChoicePointLabel() {
        return _goal_stack.currentChoicePoint().label();
    }

    /**
     * Returns an array of constrained integer variables "cards" such that cards[i] is equal to the number of variables
     * in "vars" bound to the sequental [0,vars.size()] values.
     *
     * @param vars Array of variables.
     * @return An array of constrained integer variables.
     * @throws Failure
     */
    public IntExpArray distribute(IntExpArray vars) throws Failure {
        int size = vars.size();
        int[] values = new int[size];
        for (int i = 0; i < size; ++i) {
            values[i] = i;
        }

        return distribute(vars, values);

    }

    /**
     * Returns an array of n constrained integer variables "cards" such that cards[i] is equal to the number of
     * variables in "vars" bound to the value i.
     *
     * @param vars The array of constrained ineteger expressions.
     * @param n The value to be checked.
     * @return The array of value cardinalities.
     * @throws Failure
     */
    public IntExpArray distribute(IntExpArray vars, int n) throws Failure {
        int[] values = new int[n];
        for (int i = 0; i < n; ++i) {
            values[i] = i;
        }
        return distribute(vars, values);
    }

    /**
     * Returns the array of constrained integer variables "cards" such that cards[i] is equal to the number of variables
     * in "vars" bound to the value values[i].
     *
     * @param vars The array of constrained ineteger variables.
     * @param values The array of value to be checked.
     *
     * @return The array of values cardinalities in vars.
     * @throws Failure
     */
    public IntExpArray distribute(IntExpArray vars, int[] values) throws Failure {

        IntArrayCards vcards = vars.cards();

        FastVector cards = new FastVector();

        for (int i = 0; i < values.length; ++i) {
            cards.addElement(vcards.cardAt(values[i]));
            // cards.addElement(addIntVar(0,vars.size(),"Count of "+values[i]));
        }
        // redundant constraint
        return new IntExpArray(this, cards);
    }

    /**
     * All elements of an array are associated with their domains. The union of the domains makes up the array "domain"
     * that is the set of all values that may occur in the array. An instance of {@link IntArrayCards} class is
     * associated with each instance of {@link IntExpArray}. The instance keeps track the possible number of the value
     * occurrences in the array that is it has an element(constrained integer expression) per value from the array
     * "domain". For each value from the array "domain" the method {@link #distribute(IntExpArray , IntExpArray)}
     * creates the constraint "equals" to the number of occurrences of values. The number of occurrences for each value
     * of array "domain" is specified in <code>cards</code> parameter.
     *
     * @param vars The array of constrained integer expressions.
     * @param cards The array of value occurences.
     * @return The array of value cardinalities.
     * @throws Failure
     */
    public IntArrayCards distribute(IntExpArray vars, IntExpArray cards) throws Failure {
        IntArrayCards vcards = vars.cards();

        for (int i = 0; i < vcards.cardSize(); ++i) {
            Constraint ct = vcards.cardAt(i).equals(cards.get(i));
            ct.execute();
        }

        return vcards;

    }

    /**
     * Prints the statistical information. This information is accumulated during the execution of the goals.
     */
    void doPrintInformation() {
        _out.println(
            "\nChoice Points: " + _number_of_choice_points + "  Failures: " + _number_of_failures + "  Undos: " + _number_of_undos + "  Notifications: " + _number_of_notifications + "  Memory: " + (_max_occupied_memory - _initial_memory) + "  Time: " + _execution_time + "msec");
    }

    /**
     * Executes the goal without state restoration.
     *
     * @return true if success
     * @param goal org.openl.ie.constrainer.Goal
     */
    public boolean execute(Goal goal) {
        return execute(goal, false);
    }

    /**
     * Executes the search goal provided by the first parameter. In most cases, the goal is expected to find a solution:
     * to instantiate all constrained objects and satisfied all constraints. Return true if the solution is found.
     * Returns false otherwise. The second parameter allows a user to restore the state of the constrainer after the
     * succesful execution of the main_goal.
     *
     * @return true if success
     * @param main_goal org.openl.ie.constrainer.Goal
     * @param restore_flag boolean
     */
    synchronized public boolean execute(Goal main_goal, boolean restore_flag) {
        long execution_start = System.currentTimeMillis();

        boolean success = true;

        long start_seconds = System.currentTimeMillis() / 1000;

        // save current _goal_stack
        GoalStack old_goal_stack = _goal_stack;

        _goal_stack = new GoalStack(main_goal, _reversibility_stack);

        allowUndos();

        while (!_goal_stack.empty()) {
            try {
                Goal goal = _goal_stack.popGoal();

                if (_trace_goals) {
                    _out.println("Execute: " + goal);
                }

                goal = goal.execute();
                propagate();

                if (_print_information) {
                    long occupied_memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    if (_max_occupied_memory < occupied_memory) {
                        _max_occupied_memory = occupied_memory;
                    }
                }

                if (goal != null) {
                    _goal_stack.pushGoal(goal);
                }
            } catch (Failure f) {

                if (_trace_failure_stack && _failure_display_frequency > 0 && _number_of_failures % _failure_display_frequency == 0) {
                    f.printStackTrace(_out);
                }

                if (_print_information) {
                    long occupied_memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    if (_max_occupied_memory < occupied_memory) {
                        _max_occupied_memory = occupied_memory;
                    }
                }

                clearPropagationQueue();

                // here was checking out the time limit
                // check time limit
                if (_time_limit > 0) {
                    long now_seconds = System.currentTimeMillis() / 1000;
                    if (now_seconds - start_seconds > _time_limit) {
                        // _out.println("Time Limit Violation: more than
                        // "+_time_limit+" sec");
                        // success = false;
                        throw new TimeLimitException("Time limit exceeded", f.label());
                        // break;
                    }
                }

                // check failures limit
                if (_failures_limit > 0) {
                    if (_number_of_failures > _failures_limit) {
                        _out.println("Failures Limit Violation: more than " + _failures_limit);
                        success = false;
                        break;
                    }
                }

                // Backtrack
                if (!backtrack(f.label())) {
                    success = false;
                    break;
                }
            } catch (Throwable t) {
                _out.println("Unexpected exception: " + t.toString());
                t.printStackTrace(_out);
                abort("Unexpected exception: ", t);
            }

        } // ~while

        boolean restoreAnyway = restore_flag || !success;
        if (restoreAnyway) {
            backtrackStack(_goal_stack.undoStackSize());
        }

        _execution_time += System.currentTimeMillis() - execution_start;

        if (_print_information) {
            if (!(main_goal instanceof Constraint)) {
                doPrintInformation();
            }
        }

        _goal_stack = old_goal_stack;

        return success;
    }

    /*
     * ============================================================================== High-level Components
     * ============================================================================
     */
    /**
     * Returns the expression factory for this constrainer.
     */
    public ExpressionFactory expressionFactory() {
        return _expressionFactory;
    }

    /*
     * ============================================================================== Failures
     * ============================================================================
     */
    /**
     * Throws Failure exception.
     *
     * @throws Failure
     */
    public void fail() throws Failure {
        fail("");
    }

    /**
     * Throws Failure exception.
     *
     * @param s The diagnostic message.
     * @throws Failure
     */
    public void fail(String s) throws Failure {
        fail(s, null);
    }

    /**
     * Throws Failure exception.
     *
     * @param s The diagnostic message.
     * @throws Failure
     */
    public void fail(String s, ChoicePointLabel label) throws Failure {
        _number_of_failures++;

        if (_failure_display_frequency > 0 && _number_of_failures % _failure_display_frequency == 0) {
            _out.println("Failure " + _number_of_failures + ": " + s);
        }

        if (_failure_display_frequency == 0 || _number_of_failures % _failure_display_frequency == 0) {
            for (int i = 0; i < _failure_objects.size(); i++) {
                _out.println("Failure: " + s + " " + _failure_objects.elementAt(i));
            }
        }

        /*
         * if (showInternalNames()) _failure.message(s); else _failure.message("");
         */
        throw new Failure(s, label);// _failure; //
    }

    int[] findAppropriate(FloatVar[] floatVars) {
        if (floatVars == null) {
            return null;
        }
        int size = floatVars.length;
        int[] indices = new int[size];
        java.util.HashMap map = new java.util.HashMap(size);
        Object[] vars = _floatvars.data();
        for (int i = 0; i < vars.length; i++) {
            map.put(vars[i], new Integer(i));
        }
        int validsCounter = 0;
        for (int i = 0; i < size; i++) {
            Integer idx = (Integer) (map.get(floatVars[i]));
            if (idx != null) {
                indices[validsCounter] = idx.intValue();
                validsCounter++;
            }
        }
        int[] out = new int[validsCounter];
        System.arraycopy(indices, 0, out, 0, validsCounter);
        return out;
    }

    int[] findAppropriate(IntVar[] intVars) {
        if (intVars == null) {
            return null;
        }
        int size = intVars.length;
        int[] indices = new int[size];
        java.util.HashMap map = new java.util.HashMap(size);
        Object[] vars = _intvars.data();
        for (int i = 0; i < vars.length; i++) {
            map.put(vars[i], new Integer(i));
        }
        int validsCounter = 0;
        for (int i = 0; i < size; i++) {
            Integer idx = (Integer) (map.get(intVars[i]));
            if (idx != null) {
                indices[validsCounter] = idx.intValue();
                validsCounter++;
            }
        }
        int[] out = new int[validsCounter];
        System.arraycopy(indices, 0, out, 0, validsCounter);
        return out;
    }

    FloatVar[] getFloatVars(int[] indices) {
        if (indices == null) {
            return null;
        }
        FloatVar[] ari = new FloatVar[indices.length];
        Object[] vars = _floatvars.data();
        for (int i = 0; i < indices.length; i++) {
            ari[i] = (FloatVar) vars[indices[i]];
        }
        return ari;
    }

    /**
     * functions providing the possibility of supporting multi-session added by E. Tseitlin
     */

    IntVar[] getIntVars(int[] indices) {
        if (indices == null) {
            return null;
        }
        IntVar[] ari = new IntVar[indices.length];
        Object[] vars = _intvars.data();
        for (int i = 0; i < indices.length; i++) {
            ari[i] = (IntVar) vars[indices[i]];
        }
        return ari;
    }

    /*
     * ============================================================================== EOF Tracing
     * ============================================================================
     */

    /**
     * Used internally in the implementation of subject when it sends a notificaction event. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     */
    public void incrementNumberOfNotifications() {
        _number_of_notifications++;
    }

    /**
     * This method returns the number of choice points during search execution.
     *
     * @return The number of choice points during execution.
     */
    public int numberOfChoicePoints() {
        return _number_of_choice_points;
    }

    /**
     * Sets the number of choice points. Used internally in some goals. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     *
     * @param cps Number of choice points.
     */
    public void numberOfChoicePoints(int cps) {
        _number_of_choice_points = cps;
    }

    /*
     * ============================================================================== Statistics, metrics, flags, ...
     * ============================================================================
     */
    /**
     * This method return number of failures during search execution.
     *
     * @return Number of failures during execution.
     */
    public int numberOfFailures() {
        return _number_of_failures;
    }

    /**
     * Sets the number of choice points. Used internally in some goals. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     *
     * @param nfs Number of failures.
     */
    public void numberOfFailures(int nfs) {
        _number_of_failures = nfs;
    }

    /**
     * Returns the Constrainer's output stream where the output information is printed out.
     *
     * @return the Constrainer's output stream where the output information is printed out.
     */
    public PrintStream out() {
        return _out;
    }

    /**
     * Sets the output stream where the information is printed.
     *
     * @param out the output stream to be set for Constrainer's output.
     */
    public void out(PrintStream out) {
        _out = out;
    }

    /**
     * Activates the passed constraint. This method executes the specified constraint.The constraint is not added to
     * internal constraint storage (with which {@link #addConstraint(Constraint)} and {@link #postConstraints()}
     * operates) instead it is just activated immediately.
     *
     * @param ct The constraint to be posted/executed.
     * @throws Failure When the constraint can not be posted that is the constraint is incompatible with the previously
     *             activated ones.
     */
    public void postConstraint(Constraint ct) throws Failure {
        boolean ok = execute(ct);

        if (!ok) {
            String s = "Posting of constraint failed: " + ct;
            throw new Failure(s);
        }

        // return ok;
    }

    /*
     * ============================================================================== EOF Special expressions,
     * constraints, ... ============================================================================
     */

    /**
     * Propagate events triggered by successful goal execution.
     */
    final public void propagate() throws Failure {
        while (!_propagation_queue.empty()) {
            Subject var = (Subject) _propagation_queue.pop();
            var.inProcess(false);
            var.propagate(); // may fail
        }
    }

    /**
     * Pushes the goal onto the goal stack.
     */
    void pushOnExecutionStack(Goal goal) {
        _goal_stack.pushGoal(goal);
    }

    /**
     * Sets a labeled choice point between two goals.
     */
    void setChoicePoint(Goal g1, Goal g2, ChoicePointLabel label) {
        _number_of_choice_points++;

        _goal_stack.setChoicePoint(g1, g2, label);

        allowUndos();

        if (_choice_point_objects.size() > 0) {
            printObjects(_out, "CP " + (_choice_point - 1) + ":", _choice_point_objects);
        }
    }

    /**
     * Returns true if internal names for the expressions are shown.
     *
     * @return true if internal names for the expressions are shown.
     */
    public boolean showInternalNames() {
        return _show_internal_names;
    }

    /**
     * Controls whether to show the internal names for the expressions.
     *
     * @param flag true if show.
     */
    public void showInternalNames(boolean flag) {
        _show_internal_names = flag;
    }

    /*
     * ============================================================================== EOF Execution, backtracking,
     * choice points ============================================================================
     */

    /**
     * Returns variable names printing behaviour flag.
     *
     * @return the variable names printing flag.
     */
    public boolean showVariableNames() {
        return _show_variable_names;
    }

    /**
     * @param label
     * @param restore_flag
     */
    synchronized public boolean toContinue(ChoicePointLabel label, boolean restore_flag) {
        long execution_start = System.currentTimeMillis();

        boolean success = true;

        long start_seconds = System.currentTimeMillis() / 1000;

        // save current _goal_stack
        GoalStack old_goal_stack = _goal_stack;

        // _goal_stack = new GoalStack(main_goal, _reversibility_stack);

        allowUndos();
        // Backtrack
        if (!backtrack(label)) {
            success = false;
        }

        if (success) {
            while (!_goal_stack.empty()) {
                try {
                    Goal goal = _goal_stack.popGoal();

                    if (_trace_goals) {
                        _out.println("Execute: " + goal);
                    }

                    goal = goal.execute();
                    propagate();

                    if (_print_information) {
                        long occupied_memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                        if (_max_occupied_memory < occupied_memory) {
                            _max_occupied_memory = occupied_memory;
                        }
                    }

                    if (goal != null) {
                        _goal_stack.pushGoal(goal);
                    }
                } catch (Failure f) {

                    if (_trace_failure_stack && _failure_display_frequency > 0 && _number_of_failures % _failure_display_frequency == 0) {
                        f.printStackTrace(_out);
                    }

                    if (_print_information) {
                        long occupied_memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                        if (_max_occupied_memory < occupied_memory) {
                            _max_occupied_memory = occupied_memory;
                        }
                    }

                    clearPropagationQueue();

                    // check time limit
                    if (_time_limit > 0) {
                        long now_seconds = System.currentTimeMillis() / 1000;
                        if (now_seconds - start_seconds > _time_limit) {
                            // _out.println("Time Limit Violation: more than
                            // "+_time_limit+" sec");
                            // success = false;
                            throw new TimeLimitException("time limit", f.label());
                            // break;
                        }
                    }

                    // check failures limit
                    if (_failures_limit > 0) {
                        if (_number_of_failures > _failures_limit) {
                            _out.println("Failures Limit Violation: more than " + _failures_limit);
                            success = false;
                            break;
                        }
                    }

                    // Backtrack
                    if (!backtrack(f.label())) {
                        success = false;
                        break;
                    }
                } catch (Throwable t) {
                    _out.println("Unexpected exception: " + t.toString());
                    t.printStackTrace(_out);
                    abort("Unexpected exception: ", t);
                }

            } // ~while
        }

        boolean restoreAnyway = restore_flag || !success;
        if (restoreAnyway) {
            backtrackStack(_goal_stack.undoStackSize());
        }

        _execution_time += System.currentTimeMillis() - execution_start;
        /*
         * if (_print_information) { if(!(main_goal instanceof Constraint)) doPrintInformation(); }
         */
        _goal_stack = old_goal_stack;

        return success;
    }

    /**
     * Returns the string representation of the constrainer.
     *
     * @return the string representation of the constrainer.
     */
    @Override
    public String toString() {
        return "Constrainer: " + _name + "\n" + _goal_stack + "\n" + _reversibility_stack;

    }

} // ~Constrainer
