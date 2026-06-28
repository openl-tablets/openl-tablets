package org.openl.rules.webstudio.web.trace.debug;

/**
 * A point during execution at which the engine may suspend.
 *
 * <p>Frame exits are not suspend points by themselves: when a frame returns, the caller's next
 * {@link #ENTER} or {@link #LOCATION} is the point a step lands on.
 */
public enum DebugEvent {

    /** A table frame was just entered. */
    ENTER,

    /** The current line inside the active frame changed (a cell, condition, rule, or operation). */
    LOCATION
}
