package org.openl.ie.constrainer;

/**
 * <p>
 * Title: TimeLimitException
 * </p>
 * <p>
 * Description: This kind of exception are to be thrown by Constrainer if the
 * actual time of solution searching process exceeded the time limit. The value
 * for time limit could be set by calling Constrainer.setTimeLimit(int)
 * </p>
 */

public class TimeLimitException extends RuntimeException {

    private String _msg;
    private ChoicePointLabel _label = null;

    /**
     * Constructor for a TimeLimitException with a given description and label.
     */
    public TimeLimitException(String s, ChoicePointLabel label) {
        super(s);
        _msg = s;
        _label = label;
    }

    /**
     * Returns the label.
     */
    public ChoicePointLabel label() {
        return _label;
    }

    /**
     * Returns description.
     */
    @Override
    public String toString() {
        return "Failure: " + _msg;
    }

}