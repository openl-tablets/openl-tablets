package org.openl.studio.projects.model.trace;

/**
 * How to shape a stack response: which heavy parts to build and include.
 *
 * <p>Trace stack responses can grow large — the executed tree after a profiled run, and every frame's
 * sub-steps on every step. These options let a caller keep only what it needs (for example an agent with
 * a bounded context) without changing the default the UI relies on.
 *
 * @param includeTree embed the full executed tree once the trace has finished (profiling); when false,
 *                    only the bounded profile overview is kept
 * @param profileTop  how many hotspots the profile overview returns
 * @param compact     when true, only the active (top) frame carries its sub-steps; the other frames are
 *                    the bare stack, so a step no longer re-sends every frame's steps
 */
public record StackRenderOptions(boolean includeTree, int profileTop, boolean compact) {

    /** Full detail: the whole executed tree and every frame's steps — what the UI renders. */
    public static final StackRenderOptions FULL =
            new StackRenderOptions(true, TraceDebugMapper.DEFAULT_PROFILE_TOP, false);
}
