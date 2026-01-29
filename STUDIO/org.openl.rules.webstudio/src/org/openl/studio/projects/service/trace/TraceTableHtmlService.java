package org.openl.studio.projects.service.trace;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;

/**
 * Service for rendering traced tables as HTML fragments.
 * <p>
 * This service generates HTML representations of traced tables with cell highlighting
 * to visualize which cells were accessed during rule execution. The generated HTML
 * is compatible with the React/TypeScript UI and does not depend on JSF.
 * </p>
 */
public interface TraceTableHtmlService {

    /**
     * Renders a traced table as an HTML fragment with highlighted cells.
     * <p>
     * The method retrieves the trace object for the specified node, determines
     * the associated table, applies trace filters for cell highlighting, and
     * renders the result as an HTML table element.
     * </p>
     *
     * @param traceHelper  the trace helper containing cached trace objects
     *                     for node lookup and lazy node resolution
     * @param nodeId       the trace node ID identifying which execution step to visualize
     * @param projectModel the project model providing access to filter holder
     *                     and table metadata
     * @param showFormulas if {@code true}, displays cell formulas instead of computed values;
     *                     if {@code false}, displays the computed cell values
     * @return HTML string containing a {@code <table>} element with applied trace highlighting,
     *         ready to be embedded in the UI
     * @throws org.openl.studio.common.exception.NotFoundException if the trace node
     *         or associated table cannot be found
     */
    String renderTraceTableHtml(TraceHelper traceHelper,
                                int nodeId,
                                ProjectModel projectModel,
                                boolean showFormulas);
}
