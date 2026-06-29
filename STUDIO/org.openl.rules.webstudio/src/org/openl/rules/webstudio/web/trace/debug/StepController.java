package org.openl.rules.webstudio.web.trace.debug;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;

/**
 * Decides whether execution should suspend at a given point.
 *
 * <p>Stepping is expressed as a depth threshold: execution suspends on a frame enter or a current-line
 * change whose depth is at or above the threshold (that is, {@code depth <= threshold}). Step Into uses
 * an unbounded threshold and Step Over uses the current depth. Step Out runs the current frame to
 * completion and suspends at its own {@link DebugEvent#EXIT} (so its result is on the stack), then
 * continues in the caller on the next step. Resume disables stepping and waits for a breakpoint.
 * Breakpoints (matched by table URI or table name on frame entry, or by sub-step) and an asynchronous
 * pause request always suspend, regardless of the threshold.
 *
 * <p>All mutators are synchronized so the controller thread can change breakpoints or request a pause
 * while the worker thread evaluates suspend points.
 */
final class StepController {

    /** Depth that never matches a real frame (frames are numbered from 1). */
    private static final int NEVER = 0;

    private final AtomicReference<Set<String>> breakpoints = new AtomicReference<>(Set.of());
    private int threshold = NEVER;
    private int exitDepth = NEVER;
    private volatile boolean pauseRequested;

    /** Arm the initial step before execution starts. */
    synchronized void armInitial(boolean stopAtEntry) {
        threshold = stopAtEntry ? Integer.MAX_VALUE : NEVER;
        exitDepth = NEVER;
        pauseRequested = false;
    }

    /** Arm the next step from a command issued at the given current depth. */
    synchronized void arm(DebugCommand command, int currentDepth) {
        // Into, Over and Out all suspend at the current frame's own exit, so a step that finishes the
        // frame lands on its EXIT — with the returned result on the stack — before continuing in the
        // caller. Resume runs straight to the next breakpoint.
        exitDepth = command == DebugCommand.RESUME ? NEVER : currentDepth;
        threshold = switch (command) {
            case STEP_INTO -> Integer.MAX_VALUE;
            case STEP_OVER -> currentDepth;
            case STEP_OUT -> currentDepth - 1;
            case RESUME -> NEVER;
        };
        pauseRequested = false;
    }

    void setBreakpoints(Set<String> uris) {
        this.breakpoints.set(Set.copyOf(uris));
    }

    Set<String> getBreakpoints() {
        return breakpoints.get();
    }

    /** Request an asynchronous suspend at the next safepoint. */
    void requestPause() {
        pauseRequested = true;
    }

    /**
     * Whether execution should suspend at this event.
     *
     * <p>A breakpoint is matched at table entry by URI (key {@code uri}) or by table name (key
     * {@code name}), or at a specific sub-step (key {@code uri#ref}, where {@code ref} is the current
     * line's reference such as a spreadsheet cell). A name breakpoint suspends on any same-named table,
     * since every overloaded or dimensional version shares the plain method name.
     *
     * @param event the kind of safepoint reached
     * @param depth depth of the current frame (1 for the top-level call)
     * @param uri   table URI of the current frame
     * @param ref   current sub-step reference on a location change, or {@code null}
     * @param name  table name of the current frame, for name breakpoints, or {@code null}
     */
    synchronized boolean shouldSuspend(DebugEvent event, int depth, String uri, @Nullable String ref,
                                       @Nullable String name) {
        if (pauseRequested) {
            return true;
        }
        Set<String> active = breakpoints.get();
        if (event == DebugEvent.ENTER && (active.contains(uri) || (name != null && active.contains(name)))) {
            return true;
        }
        if (event == DebugEvent.LOCATION && ref != null && active.contains(uri + "#" + ref)) {
            return true;
        }
        if (event == DebugEvent.EXIT) {
            return depth <= exitDepth;
        }
        return depth <= threshold;
    }
}
