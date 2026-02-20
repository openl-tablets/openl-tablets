package org.openl.studio.projects.service.trace;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.TraceFormatter;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.util.FileUtils;

/**
 * Default implementation of {@link TraceExportService}.
 * <p>
 * Exports trace execution results to a text file by streaming data directly
 * to a Writer without buffering in memory. This approach is essential for
 * handling large trace trees that could exceed available RAM (traces can be 1GB+).
 * </p>
 */
@Service
public class TraceExportServiceImpl implements TraceExportService {

    /**
     * Maximum time allowed for export operation (60 seconds).
     */
    private static final int MAX_WAIT_TIMEOUT = 60 * 1000;

    /**
     * Pre-allocated indentation characters for formatting nested trace levels.
     */
    private static final char[] INDENTS = new char[256];

    static {
        Arrays.fill(INDENTS, '\t');
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportTrace(TraceHelper traceHelper, Writer writer, boolean showRealNumbers)
            throws IOException, TimeoutException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_TIMEOUT;
        // Start from root node (id=0)
        ITracerObject root = traceHelper.getTableTracer(0);
        if (root == null) {
            throw new NotFoundException("trace.node.not.found.message");
        }
        printRecursive(root, traceHelper, 0, writer, deadline, showRealNumbers);
    }

    /**
     * Recursively prints trace nodes to the writer.
     *
     * @param tracer          current trace node
     * @param traceHelper     trace helper for resolving lazy nodes
     * @param level           current nesting level for indentation
     * @param writer          output writer
     * @param deadline        absolute time in milliseconds when operation should timeout
     * @param showRealNumbers whether to show exact numbers
     * @throws IOException      on write error
     * @throws TimeoutException if deadline is exceeded
     */
    private void printRecursive(ITracerObject tracer,
                                TraceHelper traceHelper,
                                int level,
                                Writer writer,
                                long deadline,
                                boolean showRealNumbers) throws IOException, TimeoutException {
        if (deadline < System.currentTimeMillis()) {
            throw new TimeoutException();
        }

        // getTableTracer initializes lazy children of this node
        Integer nodeKey = traceHelper.getNodeKey(tracer);
        if (nodeKey != null) {
            tracer = traceHelper.getTableTracer(nodeKey);
        }

        for (ITracerObject child : tracer.getChildren()) {
            writer.write(INDENTS, 0, level % INDENTS.length);
            writer.write("TRACE: ");
            writer.write(TraceFormatter.getDisplayName(child, showRealNumbers));
            writer.write('\n');
            writer.write(INDENTS, 0, level % INDENTS.length);
            writer.write("    at ");
            writer.write(FileUtils.getBaseName(child.getUri()));
            writer.write("&openl=");
            writer.write('\n');

            if (child instanceof RefToTracerNodeObject) {
                continue; // Skip references to avoid duplicate traces
            }
            printRecursive(child, traceHelper, level + 1, writer, deadline, showRealNumbers);
        }
    }
}
