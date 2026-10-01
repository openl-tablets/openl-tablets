package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;
import static org.openl.ie.constrainer.IntEvent.VALUE;

import java.util.ArrayList;
import java.util.List;

/**
 * A constrained integer expression: a variable, a constant, or an expression made of them.
 * <p>
 * Its domain is the range of values that it can take in the current state of the search. The domain narrows while the
 * search binds the variables, and widens back on backtracking.
 * <p>
 * The public methods make new expressions. The condition formulas of a decision table are compiled with the
 * parameters of this type, and OpenL binds the operators {@code +}, {@code ==}, {@code <}, {@code <=}, {@code >} and
 * {@code >=} to these methods by their names {@code add}, {@code eq}, {@code lt}, {@code le}, {@code gt} and
 * {@code ge}.
 */
public abstract class IntExp {

    /**
     * An observer and the kinds of the changes that it reacts to.
     */
    private record Subscription(int mask, Observer observer) {
    }

    final Constrainer constrainer;
    private final String name;
    private final List<Subscription> subscriptions = new ArrayList<>();

    IntExp(Constrainer constrainer) {
        this(constrainer, "");
    }

    IntExp(Constrainer constrainer, String name) {
        this.constrainer = constrainer;
        this.name = name;
    }

    /**
     * Returns the constrainer that the expression belongs to.
     */
    public Constrainer constrainer() {
        return constrainer;
    }

    /**
     * Returns the name of a variable. The name of any other expression is empty.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the expression {@code this + value}.
     */
    public IntExp add(int value) {
        return new IntExpAddValue(this, value);
    }

    /**
     * Returns the expression {@code this + exp}.
     */
    public IntExp add(IntExp exp) {
        return new IntExpAddExp(this, exp);
    }

    /**
     * Returns the expression {@code this == value}.
     */
    public IntBoolExp eq(int value) {
        return new IntBoolExpEqValue(this, value);
    }

    /**
     * Returns the expression {@code this >= value}.
     */
    public IntBoolExp ge(int value) {
        return gt(value - 1);
    }

    /**
     * Returns the expression {@code this > value}.
     */
    public IntBoolExp gt(int value) {
        return gt(new IntExpConst(constrainer, value));
    }

    /**
     * Returns the expression {@code this > exp}.
     */
    public IntBoolExp gt(IntExp exp) {
        return new IntBoolExpLessExp(exp, this);
    }

    /**
     * Returns the expression {@code this <= value}.
     */
    public IntBoolExp le(int value) {
        return lt(value + 1);
    }

    /**
     * Returns the expression {@code this < value}.
     */
    public IntBoolExp lt(int value) {
        return lt(new IntExpConst(constrainer, value));
    }

    /**
     * Returns the expression {@code this < exp}.
     */
    public IntBoolExp lt(IntExp exp) {
        return new IntBoolExpLessExp(this, exp);
    }

    /**
     * Returns the smallest value of the domain.
     */
    abstract int min();

    /**
     * Returns the largest value of the domain.
     */
    abstract int max();

    /**
     * Removes the values below the given one from the domain.
     *
     * @throws Failure if the domain becomes empty
     */
    abstract void setMin(int min) throws Failure;

    /**
     * Removes the values above the given one from the domain.
     *
     * @throws Failure if the domain becomes empty
     */
    abstract void setMax(int max) throws Failure;

    /**
     * Returns whether the domain holds a single value.
     */
    boolean bound() {
        return min() == max();
    }

    /**
     * Returns whether the domain holds the value. An expression that keeps only the bounds of its domain holds every
     * value between them.
     */
    boolean contains(int value) {
        return value >= min() && value <= max();
    }

    /**
     * Narrows the domain to the value.
     *
     * @throws Failure if the domain does not hold the value
     */
    void setValue(int value) throws Failure {
        setMin(value);
        setMax(value);
    }

    /**
     * Removes the value from the domain. A value between the bounds remains unless the expression keeps every value of
     * its domain.
     *
     * @throws Failure if the domain becomes empty
     */
    void removeValue(int value) throws Failure {
        if (value == min()) {
            setMin(value + 1);
        } else if (value == max()) {
            setMax(value - 1);
        }
    }

    /**
     * Returns the goal that binds the expression to the value for the rest of the search.
     */
    Goal equalTo(int value) {
        return () -> {
            setValue(value);
            attachObserver(VALUE | MIN | MAX, event -> {
                if (contradicts(event, value)) {
                    throw new Failure();
                }
                setValue(value);
            });
            return null;
        };
    }

    private static boolean contradicts(IntEvent event, int value) {
        return (event.isValueEvent() && event.newMin() != value) || (event.isMaxEvent() && event.newMax() < value)
                || (event.isMinEvent() && event.newMin() > value);
    }

    /**
     * Calls the observer on the changes of the domain of the kinds in the mask, until the search backtracks over this
     * call.
     *
     * @param mask the kinds of the changes, a combination of the {@link IntEvent} constants
     */
    void attachObserver(int mask, Observer observer) {
        var subscription = new Subscription(mask, observer);
        subscriptions.add(subscription);
        constrainer.addUndo(() -> subscriptions.remove(subscription));
    }

    /**
     * Calls the observers that react to the kind of the change.
     */
    final void notifyObservers(IntEvent event) throws Failure {
        for (var subscription : subscriptions) {
            if ((subscription.mask() & event.type()) != 0) {
                subscription.observer().update(event);
            }
        }
    }
}
