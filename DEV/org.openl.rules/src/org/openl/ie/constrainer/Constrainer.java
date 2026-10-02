package org.openl.ie.constrainer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.function.Supplier;

/**
 * The owner of the variables and expressions of a problem, and the search for their values.
 * <p>
 * The search goes depth-first. Every change of a variable after a choice point is undone when the search backtracks
 * to that point. Before the next step of the search, the changes of the variables are propagated to the expressions
 * that observe them.
 */
public final class Constrainer {

    /**
     * A state to backtrack to: the goal to try instead, the goals left to execute, and the size of the undo stack.
     */
    private record ChoicePoint(Goal alternative, Deque<Goal> goals, int undoSize) {
    }

    private final Deque<Runnable> undos = new ArrayDeque<>();
    private final List<IntVar> undoSavers = new ArrayList<>();
    private final SequencedSet<IntVar> propagationQueue = new LinkedHashSet<>();
    private final Deque<ChoicePoint> choicePoints = new ArrayDeque<>();
    private Deque<Goal> goals = new ArrayDeque<>();

    /**
     * Creates a variable with the values from {@code min} to {@code max}.
     */
    public IntExp addIntVar(int min, int max, String name) {
        return new IntVar(this, min, max, name);
    }

    /**
     * Creates a boolean variable.
     */
    public IntBoolVar addIntBoolVar(String name) {
        return new IntBoolVar(this, name);
    }

    /**
     * Returns the boolean expression that is always true or always false.
     */
    public IntBoolExp constant(boolean value) {
        return new IntBoolExpConst(this, value);
    }

    /**
     * Saves the action that undoes a change, to execute when the search backtracks over the change.
     */
    void addUndo(Runnable undo) {
        undos.push(undo);
    }

    /**
     * Remembers the variable that has saved its state for the current choice point.
     */
    void undoSaved(IntVar variable) {
        undoSavers.add(variable);
    }

    /**
     * Queues the changed variable to propagate its changes, unless it is in the queue already.
     */
    void enqueue(IntVar variable) {
        propagationQueue.add(variable);
    }

    /**
     * Searches for the values of the variables that satisfy the constraint, and executes the goal on them. The search
     * tries the smallest values first, so it finds the smallest values in lexicographic order.
     * <p>
     * Afterwards, all the changes are undone, including the expressions the constraint is made of, so they no longer
     * take part in the search.
     *
     * @param constraint makes the goal that imposes the constraint
     * @return {@code true} if the values are found
     */
    boolean solve(Supplier<Goal> constraint, IntExp[] vars, Goal onSolution) {
        var start = undos.size();
        goals = new ArrayDeque<>(List.of(constraint.get(), generate(vars), onSolution));
        choicePoints.clear();
        allowUndos();
        var found = true;
        while (found && !goals.isEmpty()) {
            found = executeNextGoal();
        }
        undo(start);
        return found;
    }

    /**
     * Executes the next goal, backtracking if it fails.
     *
     * @return {@code false} if the goal fails and there is no choice point to backtrack to
     */
    private boolean executeNextGoal() {
        try {
            var next = goals.pop().execute();
            propagate();
            if (next != null) {
                goals.push(next);
            }
            return true;
        } catch (Failure e) {
            propagationQueue.clear();
            return backtrack();
        }
    }

    private boolean backtrack() {
        if (choicePoints.isEmpty()) {
            return false;
        }
        var choicePoint = choicePoints.pop();
        undo(choicePoint.undoSize());
        goals = choicePoint.goals();
        goals.push(choicePoint.alternative());
        allowUndos();
        return true;
    }

    /**
     * Returns the goal that binds the variables, the first unbound one first, to their smallest values. Every binding
     * is a choice point: on backtracking, the variable loses the value and the search goes on with the next one.
     */
    private Goal generate(IntExp[] vars) {
        return () -> {
            for (var v : vars) {
                if (!v.bound()) {
                    var value = v.min();
                    setChoicePoint(() -> {
                        v.setMin(value + 1);
                        return generate(vars);
                    });
                    v.setValue(value);
                    return generate(vars);
                }
            }
            return null;
        };
    }

    private void setChoicePoint(Goal alternative) {
        choicePoints.push(new ChoicePoint(alternative, new ArrayDeque<>(goals), undos.size()));
        allowUndos();
    }

    private void propagate() throws Failure {
        while (!propagationQueue.isEmpty()) {
            propagationQueue.removeFirst().propagate();
        }
    }

    private void undo(int size) {
        while (undos.size() > size) {
            undos.pop().run();
        }
    }

    /**
     * Lets the variables save their state again, for the new choice point.
     */
    private void allowUndos() {
        undoSavers.forEach(IntVar::allowUndo);
        undoSavers.clear();
    }
}
