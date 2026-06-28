package org.openl.rules.webstudio.web.trace.debug;

/**
 * Thrown inside a parked worker to abort a debug session.
 *
 * <p>It extends {@link Error} (and is neither {@link LinkageError} nor {@link StackOverflowError}) so
 * that {@code catch} blocks in user rules and in the test runner cannot swallow it, letting the worker
 * stack unwind cleanly.
 */
public final class DebugTerminationError extends Error {

    public DebugTerminationError() {
        super("Debug session terminated", null, false, false);
    }
}
