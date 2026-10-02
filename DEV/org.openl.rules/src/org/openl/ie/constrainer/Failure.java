package org.openl.ie.constrainer;

/**
 * Signals that the current state of the search violates a constraint, so the search backtracks.
 * <p>
 * It is a control flow signal rather than an error, so it carries neither a message nor a stack trace.
 */
final class Failure extends Exception {

    Failure() {
        super(null, null, false, false);
    }
}
