package org.openl.ie.constrainer;

/**
 * A change of the domain of an integer expression: its bounds before and after the change.
 *
 * @param type the kinds of the change, a combination of {@link #VALUE}, {@link #MIN}, {@link #MAX} and
 *             {@link #REMOVE}
 */
record IntEvent(int type, int newMin, int newMax, int oldMin, int oldMax) {

    /**
     * The expression is bound to a single value.
     */
    static final int VALUE = 1;

    /**
     * The minimum has grown.
     */
    static final int MIN = 2;

    /**
     * The maximum has shrunk.
     */
    static final int MAX = 4;

    /**
     * A value between the bounds is removed.
     */
    static final int REMOVE = 8;

    /**
     * Any kind of change.
     */
    static final int ALL = VALUE | MIN | MAX | REMOVE;

    boolean isValueEvent() {
        return (type & VALUE) != 0;
    }

    boolean isMinEvent() {
        return (type & MIN) != 0;
    }

    boolean isMaxEvent() {
        return (type & MAX) != 0;
    }

    int minDiff() {
        return newMin - oldMin;
    }

    int maxDiff() {
        return newMax - oldMax;
    }

    /**
     * Returns the same change of the expression shifted by the delta.
     */
    IntEvent shift(int delta) {
        return new IntEvent(type, newMin + delta, newMax + delta, oldMin + delta, oldMax + delta);
    }
}
