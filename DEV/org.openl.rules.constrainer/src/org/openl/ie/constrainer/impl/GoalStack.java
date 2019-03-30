package org.openl.ie.constrainer.impl;

import java.io.Serializable;

import org.openl.ie.constrainer.ChoicePointLabel;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.tools.FastStack;

/**
 * An implementation of the goal stack.
 */
public final class GoalStack implements Serializable {
    /**
     * A placeholder for the information about the choice point.
     */
    static public class ChoicePoint implements java.io.Serializable {
        Goal _goal;
        ChoicePointLabel _label;
        FastStack _exeStack;
        int _undoStackSize;

        public ChoicePoint(Goal goal, ChoicePointLabel label, FastStack exeStack, int undoStackSize) {
            _goal = goal;
            _label = label;
            _exeStack = (FastStack) exeStack.clone();
            _undoStackSize = undoStackSize;
        }

        final public FastStack exeStack() {
            return _exeStack;
        }

        final public Goal goal() {
            return _goal;
        }

        final public ChoicePointLabel label() {
            return _label;
        }

        final public int undoStackSize() {
            return _undoStackSize;
        }

    } // ~ChoicePoint

    private FastStack _exeStack;
    private FastStack _choicePointStack;
    private int _undoStackSize;

    private UndoStack _undoStack;

    /**
     * Constructor with given initial goals and undo stack.
     */
    public GoalStack(FastStack initialGoals, UndoStack undoStack) {
        init((FastStack) initialGoals.clone(), undoStack);
    }

    /**
     * Constructor with given main goal and undo stack.
     */
    public GoalStack(Goal mainGoal, UndoStack undoStack) {
        FastStack initialGoals = new FastStack();
        initialGoals.push(mainGoal);
        init(initialGoals, undoStack);
    }

    /**
     * Constructor with given undo stack.
     */
    public GoalStack(UndoStack undoStack) {
        init(new FastStack(), undoStack);
    }

    /**
     * Backtracks this goal stack and the undo stack to the most recent labeled choice point.
     *
     * If the required choice point was found: - goal stack is restored to the state when the choice point was created -
     * undo stack is restored to the state when the choice point was created - choice point goal is pushed onto goal
     * stack - true is returned
     *
     * If the required choice point was not found: - goal stack will be empty - undo stack is restored to the state when
     * the goal stack was created - false is returned
     */
    final public boolean backtrack(ChoicePointLabel label) {
        ChoicePoint cp = backtrackStack(label);
        if (cp != null) {
            _undoStack.backtrack(cp.undoStackSize());
            pushGoal(cp.goal());
            return true;
        } else {
            _undoStack.backtrack(undoStackSize());
            return false;
        }
    }

    /**
     * Backtracks the state of this goal stack to the latest labeled choice point.
     *
     * If the required choice point is found: - it restores the state of the execution stack - the required choice point
     * is returned If the required choice point is not found: - the goal stack is empty - null is returned
     */
    final public ChoicePoint backtrackStack(ChoicePointLabel label) {
        while (!_choicePointStack.empty()) {
            ChoicePoint cp = (ChoicePoint) _choicePointStack.pop();
            // the condition "the required label found"
            if (label == null || label.equals(cp.label())) {
                _exeStack = cp.exeStack();
                return cp;
            }
        }

        // no choice point found -> clear the state of the goal stack
        _exeStack.clear();

        return null;
    }

    /**
     * Returns the current choice point.
     */
    final public ChoicePoint currentChoicePoint() {
        if (_choicePointStack.empty()) {
            throw new RuntimeException("No current choice point");
        }

        return (ChoicePoint) _choicePointStack.peek();
    }

    /**
     * Returns true if the execution stack is empty.
     */
    final public boolean empty() {
        return _exeStack.empty();
    }

    /**
     * Initialize this goal stack.
     */
    final void init(FastStack initialGoals, UndoStack undoStack) {
        _exeStack = initialGoals;
        _choicePointStack = new FastStack();
        _undoStack = undoStack;
        _undoStackSize = undoStack.size();
    }

    /**
     * Pops the goal from the execution stack and returns it.
     */
    final public Goal popGoal() {
        return (Goal) _exeStack.pop();
    }

    /**
     * Pushes the goal onto the execution stack.
     */
    final public void pushGoal(Goal goal) {
        _exeStack.push(goal);
    }

    /**
     * Sets a labeled choice point between two goals.
     */
    final public void setChoicePoint(Goal g1, Goal g2, ChoicePointLabel label) {
        ChoicePoint cp = new ChoicePoint(g2, label, _exeStack, _undoStack.size());
        _choicePointStack.push(cp);
        pushGoal(g1);
    }

    @Override
    public String toString() {
        return "GoalStack: " + "\n\tExecutionStack: " + _exeStack + "\n\tChiocePointStack: " + _choicePointStack;
    }

    final public int undoStackSize() {
        return _undoStackSize;
    }

} // ~GoalStack
