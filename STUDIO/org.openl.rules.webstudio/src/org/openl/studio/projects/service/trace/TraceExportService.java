package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.io.Writer;

/**
 * Exports a session's full execution trace as plain text.
 *
 * <p>The interactive debugger keeps only the live stack, so the export replays the traced run with a
 * tracer that records the whole tree, then writes it as indented {@code TRACE:} lines with each node's
 * computed value.
 */
public interface TraceExportService {

    /**
     * Write the full trace of a session's run.
     *
     * @param session      the debug session to export
     * @param writer       destination for the plain-text trace
     * @param smartNumbers keep full numeric precision instead of the default rounded format
     * @throws IOException if writing fails
     */
    void exportTrace(DebugSession session, Writer writer, boolean smartNumbers) throws IOException;
}
