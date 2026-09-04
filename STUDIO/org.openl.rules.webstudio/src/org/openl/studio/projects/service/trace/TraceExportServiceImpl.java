package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.io.Writer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.openl.studio.common.exception.NotFoundException;

/**
 * Exports a session's full trace by replaying its run with a recording tracer.
 *
 * <p>Rules are side-effect free, so replaying the same table and test case reproduces the run the user
 * started. A {@link TraceExportHook} records every table frame, cell, and decision-table check with its
 * value into a node-capped tree, which is then written as indented {@code TRACE:} lines. The cap bounds
 * memory so a large run degrades to a truncated trace instead of exhausting the heap.
 */
@Slf4j
@Component
public class TraceExportServiceImpl implements TraceExportService {

    /** Upper bound on recorded trace nodes, so a huge run cannot exhaust memory. */
    private static final int MAX_NODES = 200_000;

    @Override
    public void exportTrace(DebugSession session, Writer writer, boolean smartNumbers) throws IOException {
        var replay = session.getReplay();
        if (replay == null) {
            throw new NotFoundException("trace.execution.task.message");
        }
        var hook = new TraceExportHook(new DefaultSourceClassifier(), smartNumbers, MAX_NODES);
        replayUnderClassLoader(session.getClassLoader(), replay, hook);
        hook.writeTo(writer);
    }

    /**
     * Run the replay with the project's classloader in place. A failing rule still yields the trace up to the
     * failure — the failing frame is recorded as {@code ERROR} — so the partial tree is written either way.
     */
    private void replayUnderClassLoader(ClassLoader classLoader, TraceReplay replay, TraceExportHook hook) {
        Thread current = Thread.currentThread();
        var previous = current.getContextClassLoader();
        if (classLoader != null) {
            current.setContextClassLoader(classLoader);
        }
        try {
            replay.run(new DebugTracer(hook));
        } catch (RuntimeException | Error e) {
            log.debug("Trace export run ended with an error; writing the partial trace", e);
        } finally {
            current.setContextClassLoader(previous);
        }
    }
}
