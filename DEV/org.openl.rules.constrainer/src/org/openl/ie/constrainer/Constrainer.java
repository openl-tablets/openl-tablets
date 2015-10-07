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

import org.openl.ie.constrainer.impl.ConstraintAllDiff;
import org.openl.ie.constrainer.impl.ExpressionFactoryImpl;
import org.openl.ie.constrainer.impl.FloatVarImpl;
import org.openl.ie.constrainer.impl.FloatVarImplTrace;
import org.openl.ie.constrainer.impl.GoalStack;
import org.openl.ie.constrainer.impl.IntBoolVarImpl;
import org.openl.ie.constrainer.impl.IntExpCardIntExp;
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
 * An implementation of the Constrainer - a placeholder for all variables,
 * constraints, and search goals of the problem.
 * <p>
 * The Constrainer is a Java package for modeling and solving different
 * constraint satisfaction problems.
 *
 * A problem is represented in terms of the decision variables and constraints,
 * which define relationships between these variables.
 *
 * The decision variables could be represented in form of Java objects which may
 * use the predefined constrained variables such as IntVar.
 *
 * The constraints themselves are objects inherited from a generic class
 * Constraint.
 *
 * A user can define new business constraints.
 *
 * <p>
 * To find the problem solutions, the search algorithms could be represented
 * using objects called Goals as building blocks. The Constrainer supports a
 * reversible environment with multiple choice points: when constraints/goals
 * fail, the Constrainer automatically backtracks to a previous choice point (if
 * any).
 * <p>
 * There are several basic entities in the Constrainer:
 * <ol>
 * <li> Class Constrainer - a placeholder for all variables, constraints, and
 * search goals of the problem
 * <li> Interface Subject - a base-class for constrained variables. Contains the
 * major methods to allow constraints (observers) observe the modification of
 * the variables (subjects).
 * <li> Interface IntVar - constrained integer variables, the most popular
 * subclass of the class Subject
 * <li> Interface Goal - a base class for different search goals and
 * constraints.
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
 * GOALS EXECUTION. There are two major stacks: execution stack "EXE" and
 * alternative stack "ALT". At each choice point we create a new reversibility
 * stack "REV". EXE.push(goal); while(!EXE.empty()) { execute(EXE.pop()); Goal
 * execution could: - push new subgoal on EXE (GoalAnd) - push goals on ALT
 * (GoalOr) - fail. When failed: - pop from EXE all goals pushed on it after the
 * last choice point (done via marker) - if ALT.empty, FAILURE! -
 * EXE.push(ALT.pop()) } SUCCESS!
 *
 */

public final class Constrainer implements Serializable {
    static public final double FLOAT_MAX = 1.79769313486231570815e+308; // IEEE
                                                                        // 754
    static public final double FLOAT_MIN = 2.225073858507202e-308; // the
                                                                    // smallest
                                                                    // positive
                                                                    // IEEE 754

    static public final int INT_MAX = 2147483647;
    static public final int INT_MIN = -2147483647 - 1;

    static public double FLOAT_PRECISION = 1.0e-6;

    static private double _precision = 1e-6;
    // PRIVATE MEMBERS
    private String _name;
    private int _labelsCounter;
    private FastVector _intvars;
    private FastVector _floatvars;
    private FastVector _intsetvars;

    private FastVector _constraints;
    // private FastVector _goals;

    private int _choice_point = 0;

    // private FastStack _execution_stack;
    // private FastStack _alternative_stack;
    private GoalStack _goal_stack;

    // private FastStack _reversibility_stack;
    private UndoStack _reversibility_stack;
    private int _number_of_choice_points = 0;
    private int _number_of_failures = 0;
    private int _number_of_undos = 0;
    private long _time_limit = 0; // in seconds (0-no limit)
    private long _failures_limit = 0; // in failures (0-no limit)
    private FastVector _choice_point_objects;
    // private Failure _failure;
    private FastVector _failure_objects;
    private boolean _trace_failure_stack;

    private int _failure_display_frequency;
    private FastVector _backtrack_objects;

    // private Goal _goal_CP_marker;
    // private Undo _undo_CP_marker;
    // private Goal _restore_goal;
    // private Goal _save_goal;

    private boolean _trace_goals;

    // private ReusableFactory _undo_subject_factory;

    private boolean _show_internal_names;
    private boolean _show_variable_names;

    private long _initial_memory;
    private long _max_occupied_memory;
    private long _number_of_notifications;

    private boolean _print_information;
    private long _execution_time = 0;

    private FastQueue _propagation_queue;
    // private FastStack _propagation_queue;

    // private EventOfInterest _current_event_of_interest;

    private ExpressionFactory _expressionFactory;

    private FastStack _active_undoable_once;

    transient private PrintStream _out = System.out;

    /*
     * ==============================================================================
     * Misc: toString(), helpers, ...
     * ============================================================================
     */
    /**
     * This method aborts the program execution. It prints the "msg" and the
     * stack trace. Used to display "impossible" errors.
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
     * ReusableFactory undoSubjectFactory() { return _undo_subject_factory; }
     */

    /*
     * ==============================================================================
     * EOF High-level Components
     * ============================================================================
     */

    /**
     * Returns the precision of the constrained floating-point variable
     * calculations.
     *
     * @return the precision of the constrained floating-point variable
     *         calculations.
     */
    static public double precision() {
        return FLOAT_PRECISION;
    }

    /**
     * Sets the precision of the constrained floating-point variable
     * calculations. The default value is 1E-06.
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
     * Constructs a new constrainer - the object that serves as a placeholder
     * for all other constrained objects, constraints, and goals. Each problem
     * should define at least one Constrainer object. All other objects relate
     * to this object.
     *
     * @param s Constrainer's symbolic name
     */
    public Constrainer(String s) {
        // Debug.print("Constrainer "+s);
        _initial_memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        _max_occupied_memory = _initial_memory;
        _name = s;

        _active_undoable_once = new FastStack();

        _intvars = new FastVector();
        _floatvars = new FastVector();
        _intsetvars = new FastVector();
        _constraints = new FastVector();
        // _goals = new FastVector();

        // _execution_stack = new FastStack();
        // _alternative_stack = new FastStack();
        // _reversibility_stack = new FastStack();

        _reversibility_stack = new UndoStack();
        _goal_stack = new GoalStack(_reversibility_stack);

        _propagation_queue = new FastQueue();

        // _goal_CP_marker = new GoalDisplay(this, "ChoicePointMarker");
        // _undo_CP_marker = new UndoImpl();

        // _save_goal = new GoalDisplay(this,"Save");
        // _restore_goal = new GoalAnd(new GoalDisplay(this, "\nRestore!"),
        // new GoalFail(this));

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

    /*
     * ==============================================================================
     * Constraints
     * ============================================================================
     */
    /**
     * Adds a constraint to the Constrainer. Note: the constraint added by this
     * function should be posted afterwards by {@link #postConstraints()}
     *
     * @param ct The constraint to be added to the constrainer.
     *
     * @return The constraint passed as the parameter {@link Constraint}.
     */
    public Constraint addConstraint(Constraint ct) {
        _constraints.addElement(ct);
        addUndo(UndoFastVectorAdd.getUndo(_constraints));
        return ct;
    }

    /**
     * Adds a constraint to the Constrainer.
     *
     * Constraint is given in the form of boolean expression. Note: the
     * constraint added by this function should be posted afterwards by
     * {@link #postConstraints()}
     *
     * @param ctExp The IntBoolExp to be added to the constrainer as a
     *            constraint.
     *
     * @return The constraint made from the parameter {@link Constraint}.
     */
    public Constraint addConstraint(IntBoolExp ctExp) {
        return addConstraint(ctExp.asConstraint());
    }

    /**
     * Adds a constrained floating-point variable to the Constrainer.
     *
     * @param min The minimum possible value of variable being added.
     * @param max The maximum possible value of variable being added.
     * @return The added variable.
     */
    public FloatVar addFloatVar(double min, double max) throws Failure {
        return addFloatVar(min, max, "");
    }

    /**
     * Adds a constrained floating-point variable to the Constrainer.
     *
     * @param min The minimum possible value of the variable being added.
     * @param max The maximum possible value of the variable being added.
     * @param name Variable's symbolic name.
     * @return The added variable.
     */
    public FloatVar addFloatVar(double min, double max, String name) {
        FloatVar var = new FloatVarImpl(this, min, max, name);
        return addFloatVar(var);
    }

    /*
     * ==============================================================================
     * EOF Constraints
     * ============================================================================
     */

    /*
     * ==============================================================================
     * Goals
     * ============================================================================
     */

    // /**
    // * Adds a goal to the Constrainer.
    // *
    // * @param goal {@link Goal} to be added to the constrainer.
    // */
    // public void addGoal(Goal goal)
    // {
    // _goals.addElement(goal);
    // addUndo( UndoFastVectorAdd.getUndo(_goals) );
    // }
    //
    // /**
    // * @return All added goals.
    // */
    // public FastVector goals()
    // {
    // return _goals;
    // }
    /*
     * ==============================================================================
     * EOF Goals
     * ============================================================================
     */

    /**
     * Adds a constrained floating-point variable to the Constrainer.
     *
     * @param var Variable to add.
     * @return Added variable.
     */
    FloatVar addFloatVar(FloatVar var) {
        _floatvars.addElement(var);
        addUndo(UndoFastVectorAdd.getUndo(_floatvars));
        // addObjectToSymbolicContext(var.name(),var);
        return var;
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
     * Adds a constrained floating-point variable to the Constrainer, allows
     * trace.
     */
    public FloatVar addFloatVarTrace(double min, double max, String name, int trace) {
        FloatVar var = trace != 0 ? new FloatVarImplTrace(this, min, max, name, trace) : new FloatVarImpl(this, min,
                max, name);
        return addFloatVar(var);
    }

    /**
     * Adds an internal constrained float variable to the Constrainer,
     * selectively allows trace. Used in expressions that create internal
     * variables for their own needs. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     */
    public FloatVar addFloatVarTraceInternal(double min, double max, String name, int trace) {
        FloatVar var = trace != 0 ? new FloatVarImplTrace(this, min, max, name, trace) : new FloatVarImpl(this, min,
                max, name);
        return addFloatVarInternal(var);
    }

    /**
     * Creates and adds a constrained boolean variable to the Constrainer.
     *
     * @return The added variable.
     */
    public IntBoolVar addIntBoolVar() {
        return addIntBoolVar("");
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
     * Adds an internal constrained boolean variable to the Constrainer. Used in
     * expressions that create internal variables for their own needs. <br>
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

    /**
     * Create new constrained set variable and adds it to the constrainer
     *
     * @param values set of possible values
     * @param name Symbolic name of the new variable
     * @return variable created based on set of possible values
     */
    public IntSetVar addIntSetVar(int[] values, String name) {
        IntSetVar var = new IntSetVarImpl(this, values, name);
        return addIntSetVar(var);
    }

    /*
     * ==============================================================================
     * Variables
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
     * Adds an internal constrained set variable to the constrainer
     *
     * @param var Variable to be added
     * @return passed variable
     */
    IntSetVar addIntSetVarInternal(IntSetVar var) {
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
     * Creates an constrained integer variable from the constrained integer
     * expression and posts the "equals" constraint on them.
     *
     * @param exp The expression to be associated with the new variable.
     * @return The added variable.
     */
    public IntVar addIntVar(IntExp exp) {
        // IntVar var = addIntVar(exp.min(),exp.max(),exp.name());
        IntVar var = addIntVar(exp.min(), exp.max()); // no name to avoid
                                                        // duplicate name
        try {
            var.equals(exp).post();
        } catch (Failure f) {
            abort("Impossible failure in addIntVar(IntExp exp)");
        }

        return var;
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
     * Adds a constrained integer variable to the Constrainer, allows trace.
     */
    public IntVar addIntVarTrace(int min, int max, String name, int type, int trace) {
        IntVar var = trace != 0 ? new IntVarImplTrace(this, min, max, name, type, trace) : new IntVarImpl(this, min,
                max, name, type);
        return addIntVar(var);
    }

    /**
     * Adds an internal constrained integer variable to the Constrainer,
     * selectively allows trace. Used in expressions that create internal
     * variables for their own needs. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     */
    public IntVar addIntVarTraceInternal(int min, int max, String name, int type, int trace) {
        IntVar var = trace != 0 ? new IntVarImplTrace(this, min, max, name, type, trace) : new IntVarImpl(this, min,
                max, name, type);
        return addIntVarInternal(var);
    }

    /*
     * ==============================================================================
     * Propagation
     * ============================================================================
     */
    /**
     * Adds the subject (usually variable) to the propagation queue. It happenes
     * when the subject changes its state. The notificatioin events will be
     * generated in {@link #propagate} method.
     */
    public void addToPropagationQueue(Subject subject) {
        _propagation_queue.push(subject);
    }

    /*
     * ==============================================================================
     * Undo objects
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
     * Adds an undo-object to the reversibility stack for a given undoable
     * object. Some undo-objects can be generated one time between choice
     * points. Constrainer notifies such objects when backtrack or choice point
     * occures.
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

    /**
     * Adds an undoable action. Undoable action is executed during backtracking.
     * It has not change constrainer state (modify variables, add constraints,
     * etc.). It can be used to animate search process. For example, you draw a
     * square during the search and erase it in undoable action during
     * backtracking.
     *
     * @param goal Undoable goal to add.
     */
    public void addUndoableAction(Goal goal) {
        addUndo(UndoableAction.getUndo(goal));
        allowUndos(); // the action should have the state at the time when it
                        // was added
    }

    /*
     * ==============================================================================
     * EOF Variables
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
     * Adds an undoable float to the Constrainer.
     *
     * @param value Initial value.
     * @param name Symbolic name.
     * @return Added undoable float.
     */
    public UndoableFloat addUndoableFloat(double value, String name) {
        return new UndoableFloatImpl(this, value, name);
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

    /*
     * ==============================================================================
     * Undoable values
     * ============================================================================
     */
    /**
     * Adds an undoable integer to the Constrainer.
     *
     * @param value Initial value.
     * @param name Symbolic name.
     * @return Added undoable integer.
     */
    public UndoableInt addUndoableInt(int value, String name) {
        return new UndoableIntImpl(this, value, name);
    }

    /*
     * ==============================================================================
     * EOF Undoable values
     * ============================================================================
     */

    /**
     * Creates "All Different" constraint.
     *
     * @param intvars The array of constrained integer variables.
     * @return Constraint stating that all variables in the specified array must
     *         be different.
     */
    public Constraint allDiff(IntExpArray intvars) {
        return new ConstraintAllDiff(intvars);
    }

    /**
     * Clears the undone-flags for active undoable once objects. This force them
     * to create undos again. Used: - when a choice point is set - when
     * backtracking is performed
     */
    void allowUndos() {
        while (!_active_undoable_once.empty()) {
            ((UndoableOnceImpl) _active_undoable_once.pop()).restore();
        }
    }

    /*
     * ==============================================================================
     * Execution, backtracking, choice points
     * ============================================================================
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

    /*
     * ==============================================================================
     * EOF Undo objects
     * ============================================================================
     */

    /**
     * Returns a constrained integer expression that is equal to the number of
     * variables in "vars" bound to the "value".
     *
     * @param intvars The array of constrained integer expressions.
     * @param value The value to be checked.
     * @return The constrained integer expression that is equal to the number of
     *         variables in "vars" bound to the "value".
     * @throws Failure
     */
    public IntExp cardinality(IntExpArray intvars, int value) throws Failure {
        // IntVar card = addIntVar(0,intvars.size(),"Count of "+value);
        // ConstraintCardinality ct = new
        // ConstraintCardinality(intvars,value,card);
        // ct.execute(); // may fail
        // return card;

        return intvars.cards().cardAt(value);

    }

    /**
     * Returns context for this constrainer.
     */
    // Context constrainerContext()
    // {
    // Context cxt1 = new ObjectContext(getClass(),this);
    // Context cxt2 = new ConstrainerContext(this);
    // cxt2.setParent(cxt1);
    // return cxt2;
    // }
    /**
     * Evaluate a code written in the symbolic form.
     */
    // Object evaluateSpl(String s) throws Exception
    // {
    // s = s + ";";
    // Context cxt = constrainerContext();
    // Algebra algebra = Algebra.constrainerAlgebra();
    //
    // SplCode code = new SplCode(s);
    // code.validate(cxt,algebra);
    // return code.evaluate(cxt, algebra);
    // }
    /**
     * Evaluate a constraint written in the symbolic form.
     */
    // public Constraint evaluateConstraint(String s) throws Exception
    // {
    // Object result = evaluateSpl(s);
    //
    // if(result instanceof IntBoolExp)
    // {
    // return ((IntBoolExp)result).asConstraint();
    // }
    // else if(result instanceof Constraint)
    // {
    // return (Constraint)result;
    // }
    // else
    // {
    // throw new Exception( "Invalid constraint."
    // + " String: '" + s + "'."
    // + " Result: '" + result + "'." );
    // }
    //
    // }
    /**
     * Adds a constraint written in the symbolic form. Note: the constraint
     * added by this function should be posted afterwards by
     * {@link #postConstraints()}
     *
     * @param s The string representing constraint.
     *
     * @return The constraint made from the parameter {@link Constraint}.
     */
    // public Constraint addConstraint(String s) throws Exception
    // {
    // Constraint ct = evaluateConstraint(s);
    // addConstraint(ct);
    // return ct;
    // }
    /**
     * Posts a constraint written in the symbolic form. Makes passed constraint
     * active. This method executes specified constraint given in the form of
     * boolean expression. The constraint is not being added to internal
     * constraint storage (with which {@link #addConstraint(Constraint)} and
     * {@link #postConstraints()} operate, but is just being activated
     * immediately.
     *
     * @param s String represented constraint.
     * @throws Exception When some constraint cannot be posted (constraint is
     *             incompatible with others) or when string is invalid.
     */
    // void postConstraint(String s) throws Exception
    // {
    // Constraint ct = evaluateConstraint(s);
    // postConstraint(ct);
    // }
    /*
     * ==============================================================================
     * EOF Symbolic expressions
     * ============================================================================
     */
    /*
     * extending to 5.1.0 added by S. Vanskov
     */
    /**
     * Returns a constrained integer expression that is equal to the number of
     * variables in <code> array </code> bound to the value of
     * <code> exp <code>.
     *
     * @param array The array of constrained integer expressions.
     * @param exp The constrained integer expression to be checked.
     *
     * @return The constrained integer expression that is equal to the number
     * of variables in <code> array </code> bound to the value of <code> exp <code>.
     *
     * @throws Failure
     */
    public IntExp cardinality(IntExpArray array, IntExp exp) throws Failure {
        return new IntExpCardIntExp(array, exp);
    }

    /*
     * ==============================================================================
     * EOF Limits
     * ============================================================================
     */

    /**
     * Returns expression being equal to the number of required values in a set
     * domain
     *
     * @param var constrained set variable
     * @return var.cardinality() expression
     */
    public IntExp cardinality(IntSetVar var) {
        return var.cardinality();
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
     * Returns an array of constrained integer variables "cards" such that
     * cards[i] is equal to the number of variables in "vars" bound to the
     * sequental [0,vars.size()] values.
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
     * Returns an array of n constrained integer variables "cards" such that
     * cards[i] is equal to the number of variables in "vars" bound to the value
     * i.
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
     * Returns the array of constrained integer variables "cards" such that
     * cards[i] is equal to the number of variables in "vars" bound to the value
     * values[i].
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
     * All elements of an array are associated with their domains. The union of
     * the domains makes up the array "domain" that is the set of all values
     * that may occur in the array. An instance of {@link IntArrayCards} class
     * is associated with each instance of {@link IntExpArray}. The instance
     * keeps track the possible number of the value occurrences in the array
     * that is it has an element(constrained integer expression) per value from
     * the array "domain". For each value from the array "domain" the method
     * {@link #distribute(IntExpArray , IntExpArray)} creates the constraint
     * "equals" to the number of occurrences of values. The number of
     * occurrences for each value of array "domain" is specified in
     * <code>cards</code> parameter.
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
     * Prints the statistical information. This information is accumulated
     * during the execution of the goals.
     */
    void doPrintInformation() {
        _out.println("\nChoice Points: " + _number_of_choice_points + "  Failures: " + _number_of_failures
                + "  Undos: " + _number_of_undos + "  Notifications: " + _number_of_notifications + "  Memory: "
                + (_max_occupied_memory - _initial_memory) + "  Time: " + _execution_time + "msec");
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
     * Executes the search goal provided by the first parameter. In most cases,
     * the goal is expected to find a solution: to instantiate all constrained
     * objects and satisfied all constraints. Return true if the solution is
     * found. Returns false otherwise. The second parameter allows a user to
     * restore the state of the constrainer after the succesful execution of the
     * main_goal.
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

                if (_trace_failure_stack && _failure_display_frequency > 0
                        && _number_of_failures % _failure_display_frequency == 0) {
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
            _reversibility_stack.backtrack(_goal_stack.undoStackSize());
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

    /**
     * Executes the search goal provided by the first parameter to find a
     * solution with the "solution_number".
     *
     * @param main_goal Goal to execute.
     * @param solution_number Solution to find.
     * @return true if success
     */
    public boolean execute(Goal main_goal, int solution_number) {
        return execute(main_goal, solution_number, false);
    }

    /**
     * Executes the search goal provided by the first parameter to find a
     * solution with the "solution_number".
     *
     * @param main_goal Goal to execute
     * @param solution_number Number of desired solution
     * @param restore_flag Restoration flag.
     * @return true if success
     */
    public boolean execute(Goal main_goal, int solution_number, boolean restore_flag) {
        Goal goal = new GoalAnd(main_goal, new GoalCheckSolutionNumber(this, solution_number));
        return execute(goal, restore_flag);
    }

    /*
     * ==============================================================================
     * EOF Statistics, metrics, flags, ...
     * ============================================================================
     */

    /*
     * ==============================================================================
     * Tracing
     * ============================================================================
     */

    /**
     * Executes the search goal provided by the first parameter to find all the
     * problem solutions. Each solution should be saved/printed inside the
     * main_goal. After the goal execution the state of the constrainer is
     * restored.
     *
     * @param main_goal The goal to execute.
     * @return true if the goal has been successfully executed.
     */
    public boolean executeAll(Goal main_goal) {
        Goal all_solutions = new GoalAllSolutions(main_goal);
        return execute(all_solutions);
    }

    /**
     * Return the search execution time.
     *
     * @return The search execution time in milliseconds.
     */
    public long executionTime() {
        return _execution_time;
    }

    /*
     * ==============================================================================
     * High-level Components
     * ============================================================================
     */
    /**
     * Returns the expression factory for this constrainer.
     */
    public ExpressionFactory expressionFactory() {
        return _expressionFactory;
    }

    /*
     * ==============================================================================
     * Failures
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
         * if (showInternalNames()) _failure.message(s); else
         * _failure.message("");
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

    /**
     * Returns all float constrained variables. The set of added variables is
     * reversible.
     *
     * @return All float constrained variables.
     */
    public FastVector floats() {
        return _floatvars;
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
     * functions providing the possibility of supporting multi-session added by
     * E. Tseitlin
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

    /**
     * Returns a number of the last solution (total amount of available
     * solutions) Checks from 0 to max_sol
     *
     * @param max_sol Maximum solution number to check.
     * @return Nuber of the last solution or max_sol
     */
    public int getSolutionsNumber(Goal main_goal, int max_sol) {
        GoalCheckSolutionNumber check = new GoalCheckSolutionNumber(this, max_sol);
        Goal goal = new GoalAnd(main_goal, check);
        if (execute(goal, true)) {
            return max_sol;
        } else {
            return check.getCurrentSolutionNumber();
        }
    }

    /*
     * ==============================================================================
     * EOF Tracing
     * ============================================================================
     */

    /**
     * @return A goal that executes a set of constraints. Or null if there are
     *         no constraints to post.
     *
     * @param constraints An array of Constraints.
     */
    Goal GoalPostConstraints(Object[] constraints) {
        Goal g = null;
        for (int i = 0; i < constraints.length; i++) {
            Object o = constraints[i];
            Constraint ct;
            if (o instanceof Constraint) {
                ct = (Constraint) o;
            } else if (o instanceof IntBoolExp) {
                ct = ((IntBoolExp) o).asConstraint();
            } else {
                throw new RuntimeException("Not a constraint: " + o);
            }

            if (ct == null) {
                continue;
            }

            if (i == 0) {
                g = ct;
            } else {
                g = new GoalAnd(g, ct);
            }
        }

        return g;
    }

    /**
     * Used internally in the implementation of subject when it sends a
     * notificaction event. <br>
     * <b>Note:</b>Constrainer's users should not use this method.
     */
    public void incrementNumberOfNotifications() {
        _number_of_notifications++;
    }

    /**
     * This method returns all integer constrained variables were ever being
     * added. The set of added variables is reversible.
     *
     * @return All integer constrained variables.
     */
    public FastVector integers() {
        return _intvars;
    }

    /*
     * ==============================================================================
     * Special expressions, constraints, ...
     * ============================================================================
     */
    /**
     * Returns constrained set being intersection of two sets variables passed
     *
     * @param var1 first constrained set variable
     * @param var2 second constrained set variable
     * @return var1.intersectionWith(var2)
     */
    public IntSetVar intersection(IntSetVar var1, IntSetVar var2) {
        return var1.intersectionWith(var2);
    }

    /**
     * States that two sets has empty intersection
     *
     * @param var1 first constrained set variable
     * @param var2 second constrained set variable
     * @return var1.nullIntersectionWith(var2) constraint
     */
    public Constraint nullIntersect(IntSetVar var1, IntSetVar var2) {
        return var1.nullIntersectWith(var2);
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
     * ==============================================================================
     * Statistics, metrics, flags, ...
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
     * This method returns the number of notifications occured during search
     * execution.
     *
     * @return The number of notifications.
     */
    public long numberOfNotifications() {
        return _number_of_notifications;
    }

    /**
     * Returns the Constrainer's output stream where the output information is
     * printed out.
     *
     * @return the Constrainer's output stream where the output information is
     *         printed out.
     */
    public PrintStream out() {
        return _out;
    }

    /**
     * Returns an array of constrained integer variables "cards" such that
     * cards[i] is equal to the number of variables in "vars" bound to the value
     * values[i].
     */

    /*
     * public Vector distribute(Vector vars, int[] values) throws Failure {
     * Vector cards = new Vector(); for(int i=0; i< values.length; ++i) {
     * cards.addElement(cardinality(vars,values[i]));
     * //cards.addElement(addIntVar(0,vars.size(),"Count of "+values[i])); } //
     * redundant constraint IntExp card_sum = new IntExpAddVector(this, cards);
     * card_sum.less(vars.size()+1).execute(); return cards; }
     */

    /**
     * Sets the output stream where the information is printed.
     *
     * @param out the output stream to be set for Constrainer's output.
     */
    public void out(PrintStream out) {
        _out = out;
    }

    /**
     * Activates the passed constraint. This method executes the specified
     * constraint.The constraint is not added to internal constraint storage
     * (with which {@link #addConstraint(Constraint)} and
     * {@link #postConstraints()} operates) instead it is just activated
     * immediately.
     *
     * @param ct The constraint to be posted/executed.
     * @throws Failure When the constraint can not be posted that is the
     *             constraint is incompatible with the previously activated
     *             ones.
     */
    public void postConstraint(Constraint ct) throws Failure {
        boolean ok = execute(ct);

        if (!ok) {
            String s = "Posting of constraint failed: " + ct;
            throw new Failure(s);
        }

        // return ok;
    }

    /**
     * Activates the passed constraint. This method executes the specified
     * constraint provided in the form of boolean expression. The constraint is
     * not added to internal constraint storage (with which
     * {@link #addConstraint(Constraint)} and {@link #postConstraints()}
     * operates) instead it is just activated immediately.
     *
     * @param ct The constraint to post/execute.
     * @throws Failure When the constraint cannot be posted that is the
     *             constraint is incompatible with the previously activated
     *             ones.
     */
    public void postConstraint(IntBoolExp ct) throws Failure {
        postConstraint(ct.asConstraint());
    }

    /**
     * Activates all the added with {@link #addConstraint(Constraint)} and not
     * activated constraints.
     *
     * @throws Failure When the constraints added with
     *             {@link #addConstraint(Constraint)} are incompatible with the
     *             previously activated ones or together so they can not be
     *             posted.
     */
    public void postConstraints() throws Failure {
        postConstraints(_constraints.toArray());
    }

    /*
     * ==============================================================================
     * EOF Special expressions, constraints, ...
     * ============================================================================
     */

    /**
     * Posts the set of constraints (or/and IntBoolExp) represented as an array
     * of java.lang.Object. All constraints are being executed immediately.
     *
     * @param constraints An array of Constraints to post/execute.
     * @throws Failure When some constraint cannot be posted (constraint is
     *             incompatible with others)
     */
    void postConstraints(Object[] constraints) throws Failure {
        Goal g = GoalPostConstraints(constraints);

        if (g == null) {
            return;
        }

        boolean ok = execute(g);

        if (!ok) {
            String s;
            if (constraints.length == 1) {
                s = "Posting of constraint failed: " + constraints[0];
            } else {
                s = "Posting of " + constraints.length + " constraints failed";
            }

            throw new Failure(s);
        }

        // return ok;
    }

    /**
     * Enables printing the internal constrainer's information.
     */
    public void printInformation() {
        _print_information = true;
    }

    /**
     * Propagate events triggered by successful goal execution.
     */
    final public void propagate() throws Failure {
        while (!_propagation_queue.empty()) {
            Subject var = (Subject) _propagation_queue.pop();
            var.inProcess(false);
            var.propagate(); // may fail
            // if (!var.inProcess())
            // var.clearPropagationEvents();
        }
    }

    /**
     * Pushes the goal onto the goal stack.
     */
    void pushOnExecutionStack(Goal goal) {
        _goal_stack.pushGoal(goal);
    }

    /**
     * Returns the constrained floating-point expression that equals to the
     * scalar product of the array of constrained floating-point expressions and
     * the array of <b>double</b> values. The expression has the following
     * symbolic form: exps[0]*values[0] + exps[1]*values[1] + ...
     *
     * @param exps The array of constrained floating-point expressions.
     * @param values The array of <b>double</b> values.
     * @return The scalar product constrained floating-point expression.
     */
    public FloatExp scalarProduct(FloatExpArray exps, double[] values) {
        int size = exps.size();

        if (values.length != size) {
            throw new RuntimeException("scalarProduct parameters have different size");
        }

        FloatExpArray products = new FloatExpArray(this, size);
        for (int i = 0; i < size; i++) {
            products.set(exps.elementAt(i).mul(values[i]), i);
        }

        return products.sum();
    }

    /**
     * Returns the constrained floating-point expression that equals to the
     * scalar product of the array of constrained integer expressions and the
     * array of <b>double</b> values. The expression has the following symbolic
     * form: exps[0]*values[0] + exps[1]*values[1] + ...
     *
     * @param exps The array of constrained integer expressions.
     * @param values The array of <b>double</b> values.
     * @return The scalar product constrained floating-point expression.
     */
    public FloatExp scalarProduct(IntExpArray exps, double[] values) {
        int size = exps.size();

        if (values.length != size) {
            throw new RuntimeException("scalarProduct parameters have different size");
        }

        FloatExpArray products = new FloatExpArray(this, size);
        for (int i = 0; i < size; i++) {
            products.set(exps.elementAt(i).mul(values[i]), i);
        }

        return products.sum();
    }

    /**
     * Returns the constrained integer expression that equals to the scalar
     * product of the array of constrained integer expressions and the array of
     * <b>int</b> values. The expression has the following symbolic form:
     * exps[0]*values[0] + exps[1]*values[1] + ...
     *
     * @param exps The array of constrained integer expressions.
     * @param values The array of <b>int</b> values.
     * @return The scalar product constrained integer expression.
     */
    public IntExp scalarProduct(IntExpArray exps, int[] values) {
        int size = exps.size();

        if (values.length != size) {
            throw new RuntimeException("scalarProduct parameters have different size");
        }

        IntExpArray products = new IntExpArray(this, size);
        for (int i = 0; i < size; i++) {
            products.set(exps.elementAt(i).mul(values[i]), i);
        }

        return products.sum();
    }

    /**
     * Sets a choice point between two goals.
     */
    void setChoicePoint(Goal g1, Goal g2) {
        setChoicePoint(g1, g2, null);
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
     * Sets the limit for the number of failures during a goal execution. 0
     * means no failures limits (default).
     *
     * @param nf The number of failures to be set as the limit.
     */
    public void setFailuresLimit(long nf) {
        _failures_limit = nf;
    }

    /*
     * ==============================================================================
     * Limits
     * ============================================================================
     */
    /**
     * Sets the time limit for the search execution (in seconds). 0 means no
     * time limit (default).
     *
     * @param seconds The time limit to be set.
     */
    public void setTimeLimit(long seconds) {
        _time_limit = seconds;
    }

    /**
     * Returns true if failure tracing is on.
     *
     * @return true if failure tracing is on.
     */
    public boolean showFailures() {
        return (_failure_display_frequency > 0);
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
     * ==============================================================================
     * EOF Execution, backtracking, choice points
     * ============================================================================
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
     * Changes variable names printing behaviour.
     *
     * @param flag Boolean parameter. To enable printing should be set to true
     *            and otherwise to false.
     */
    public void showVariableNames(boolean flag) {
        _show_variable_names = flag;
    }

    /**
     * Returns expression: sum of passed expressions: var1.add(var2)
     *
     * @param var1 First expression
     * @param var2 Second expression
     * @return var1.add(var2) expression
     */
    public IntExp sum(IntExp var1, IntExp var2) {
        return var1.add(var2);
    }

    /*
     * ==============================================================================
     * EOF Propagation
     * ============================================================================
     */

    /**
     * Returns the constrained integer expression that equals to the sum of all
     * expressions from the array.
     *
     * @param vars The array of constrained integer expressions to sum.
     * @return the constrained integer expression equal to the sum.
     */
    public IntExp sum(IntExpArray vars) {
        /*
         * int min = 0; int max = 0; for(int i=0; i < vars.size(); ++i) { IntVar
         * var = (IntVar)vars.elementAt(i); min += var.min(); max += var.max(); }
         * IntVar result = addIntVar(min,max); execute(new
         * ConstraintAddVector(vars,result)); return result;
         */
        return vars.sum();
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

                    if (_trace_failure_stack && _failure_display_frequency > 0
                            && _number_of_failures % _failure_display_frequency == 0) {
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
            _reversibility_stack.backtrack(_goal_stack.undoStackSize());
        }

        _execution_time += System.currentTimeMillis() - execution_start;
        /*
         * if (_print_information) { if(!(main_goal instanceof Constraint))
         * doPrintInformation(); }
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

    /*
     * ==============================================================================
     * EOF Failures
     * ============================================================================
     */

    /**
     * The function turns on the tracing for the array of the constrained
     * integer expressions. The array is printed to {@link #out()} every time
     * when at least one variable from this vector is modified.
     *
     * @param vars the array to be watched for modifications.
     */
    public void trace(IntExpArray vars) {
        class ObserverTraceVars extends Observer {
            private IntExpArray _vars;

            ObserverTraceVars(IntExpArray vars) {
                _vars = vars;
            }

            @Override
            public Object master() {
                return Constrainer.this;
            }

            @Override
            public int subscriberMask() {
                return EventOfInterestConstants.ALL;
            }

            @Override
            public void update(Subject var, EventOfInterest interest) throws Failure {
                _out.println("Trace " + interest + ": " + _vars);
            }

        } // ~ ObserverTraceVars

        Observer observer = new ObserverTraceVars(vars);
        for (int i = 0; i < vars.size(); i++) {
            Subject var = vars.get(i);
            var.attachObserver(observer);
        }
    }

    /**
     * The function turns on the tracing for the subject. The expression is
     * printed to {@link #out()} every time when subject send notification
     * event.<br>
     * For example, when domain of the consstraint expression changed.
     *
     * @param subject the expression to be watched for modifications.
     */
    public void trace(Subject subject) {
        subject.trace();
    }

    /**
     * Performs tracing during backtracking.
     *
     * @param obj Object that is printed using toString() method.
     */
    public void traceBacktracks(Object obj) {
        _backtrack_objects.addElement(obj);
    }

    /**
     * Turn on tracing of choice points. Object is printed to {@link #out()}
     * when choice point occures.
     *
     * @param obj Object to print.
     */
    public void traceChoicePoints(Object obj) {
        _choice_point_objects.addElement(obj);
    }

    /**
     * Turn on tracing of the goal execution.
     */
    public void traceExecution() {
        showInternalNames(true);
        _trace_goals = true;
    }

    /**
     * Turns on the tracing of every failure.
     */
    public void traceFailures() {
        traceFailures(1);
    }

    /**
     * Turns on the tracing of the failures.
     *
     * @param frequency Frequency of the tracing.
     */
    public void traceFailures(int frequency) {
        traceFailures(frequency, null);
    }

    /*
     * ==============================================================================
     * EOF Misc: helpers,...
     * ============================================================================
     */

    /*
     * ==============================================================================
     * Symbolic context and expressions
     * ============================================================================
     */

    // /**
    // * Adds an internal object to the symbolic context.
    // */
    // void addInternalObjectToSymbolicContext(Object o)
    // {
    // try
    // {
    // symbolicContext().addInternalVar(o);
    // }
    // catch(Exception e)
    // {
    // _out.println(e.getMessage());
    // }
    // }
    // /**
    // * Adds named object to the symbolic context.
    // */
    // void addObjectToSymbolicContext(String name, Object o)
    // {
    // try
    // {
    // symbolicContext().setVar(name,o);
    // }
    // catch(Exception e)
    // {
    // _out.println(e.getMessage());
    // }
    // }

    /**
     * Turns on the tracing of the failures. Object is printed to {@link #out()}
     * when failure occures.
     *
     * @param frequency Frequency of the tracing.
     * @param obj Object to print.
     */
    public void traceFailures(int frequency, Object obj) {
        _failure_display_frequency = frequency;
        if (obj != null) {
            _failure_objects.addElement(obj);
        }
    }

    /* EO additions */

    /**
     * Turns on the tracing of every failure. Object is printed to
     * {@link #out()} when failure occures.
     *
     * @param obj Object to print.
     */
    public void traceFailures(Object obj) {
        traceFailures(1, obj);
    }

    /**
     * Tells the constrainer to display the Java stack trace when a failure
     * occurs. To control the frequency of such displaying, use
     * traceFailres(frequency). For example, to display the stack on failure#25,
     * use: C.traceFailureStack(); C.traceFailures(25);
     */
    public void traceFailureStack() {
        _trace_failure_stack = true;
    }

    /**
     * The function turns off all tracings for the Constrainer.
     */
    public void traceOff() {
        showInternalNames(false);
        _trace_goals = false;
        _backtrack_objects.clear();
        _choice_point_objects.clear();
        _failure_objects.clear();
        _trace_failure_stack = false;
        _failure_display_frequency = 0;
    }

    /**
     * Returns constrained set variable being union of two sets
     *
     * @param var1 first constrained set variable
     * @param var2 second constrained set variable
     * @return var1.unionWith(var2)
     */
    public IntSetVar union(IntSetVar var1, IntSetVar var2) {
        return var1.unionWith(var2);
    }

} // ~Constrainer
