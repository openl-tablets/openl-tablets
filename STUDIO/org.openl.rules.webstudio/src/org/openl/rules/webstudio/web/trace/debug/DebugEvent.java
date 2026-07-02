package org.openl.rules.webstudio.web.trace.debug;

/**
 * A point during execution at which the engine may suspend.
 */
public enum DebugEvent {

    /** A table frame was just entered. */
    ENTER,

    /** The current line inside the active frame changed (a cell, condition, rule, or operation). */
    LOCATION,

    /**
     * A table frame has finished and is about to return. A Step Out lands here so the user can inspect
     * the returning frame's result before it leaves the stack.
     */
    EXIT
}
