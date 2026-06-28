package org.openl.rules.webstudio.web.trace.debug;

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Decides whether execution should suspend at a given point.
 *
 * <p>Stepping is expressed as a single depth threshold: execution suspends on a frame enter or a
 * current-line change whose depth is at or above the threshold (that is, {@code depth <= threshold}).
 * Step Into uses an unbounded threshold, Step Over uses the current depth, and Step Out uses one less.
 * Resume disables stepping and waits for a breakpoint. Breakpoints (matched by table URI on frame
 * entry) and an asynchronous pause request always suspend, regardless of the threshold.
 *
 * <p>All mutators are synchronized so the controller thread can change breakpoints or request a pause
 * while the worker thread evaluates suspend points.
 */
final class StepController {

    /** Depth that never matches a real frame (frames are numbered from 1). */
    private static final int NEVER = 0;

    private volatile Set<String> breakpoints = Set.of();
    private int threshold = NEVER;
    private volatile boolean pauseRequested;

    /** Arm the initial step before execution starts. */
    synchronized void armInitial(boolean stopAtEntry) {
        threshold = stopAtEntry ? Integer.MAX_VALUE : NEVER;
        pauseRequested = false;
    }

    /** Arm the next step from a command issued at the given current depth. */
    synchronized void arm(DebugCommand command, int currentDepth) {
        threshold = switch (command) {
            case STEP_INTO -> Integer.MAX_VALUE;
            case STEP_OVER -> currentDepth;
            case STEP_OUT -> currentDepth - 1;
            case RESUME -> NEVER;
        };
        pauseRequested = false;
    }

    void setBreakpoints(Set<String> uris) {
        this.breakpoints = Set.copyOf(uris);
    }

    Set<String> getBreakpoints() {
        return breakpoints;
    }

    /** Request an asynchronous suspend at the next safepoint. */
    void requestPause() {
        pauseRequested = true;
    }

    /**
     * Whether execution should suspend at this event.
     *
     * <p>A breakpoint is matched either at table entry (key {@code uri}) or at a specific sub-step (key
     * {@code uri#ref}, where {@code ref} is the current line's reference such as a spreadsheet cell).
     *
     * @param event the kind of safepoint reached
     * @param depth depth of the current frame (1 for the top-level call)
     * @param uri   table URI of the current frame
     * @param ref   current sub-step reference on a location change, or {@code null}
     */
    synchronized boolean shouldSuspend(DebugEvent event, int depth, String uri, @Nullable String ref) {
        if (pauseRequested) {
            return true;
        }
        if (event == DebugEvent.ENTER && breakpoints.contains(uri)) {
            return true;
        }
        if (event == DebugEvent.LOCATION && ref != null && breakpoints.contains(uri + "#" + ref)) {
            return true;
        }
        return depth <= threshold;
    }
}
