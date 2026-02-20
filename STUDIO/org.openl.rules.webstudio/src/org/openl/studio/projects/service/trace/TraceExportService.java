package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeoutException;

import org.openl.rules.ui.TraceHelper;

/**
 * Service for exporting trace execution results to a text file.
 * <p>
 * This service streams trace data directly to a Writer without buffering in memory,
 * making it suitable for large trace trees that could exceed available RAM.
 * </p>
 */
public interface TraceExportService {

    /**
     * Exports the complete trace tree to the provided writer.
     * <p>
     * The method traverses the trace tree starting from the root node (id=0),
     * resolving lazy nodes during traversal using the provided TraceHelper.
     * Output is written directly to the writer without intermediate buffering.
     * </p>
     *
     * @param traceHelper     trace helper containing the cached trace tree and
     *                        responsible for resolving lazy nodes during traversal
     * @param writer          output writer (typically from HttpServletResponse)
     *                        to which trace data will be streamed
     * @param showRealNumbers if {@code true}, shows exact numeric values;
     *                        if {@code false}, shows formatted/rounded values
     * @throws IOException      if an I/O error occurs while writing to the writer
     * @throws TimeoutException if the export operation exceeds the maximum allowed time
     *                          (typically 60 seconds) to prevent infinite loops
     */
    void exportTrace(TraceHelper traceHelper, Writer writer, boolean showRealNumbers)
            throws IOException, TimeoutException;
}
