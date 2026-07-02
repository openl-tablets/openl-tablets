package org.openl.studio.projects.service.trace;

import java.util.List;

import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.studio.projects.model.trace.CellHighlight;

/**
 * Computes the cells to highlight on a traced table.
 *
 * <p>The client fetches the table's raw grid from the Tables API and overlays these highlights by A1
 * cell address, so the traced table renders entirely on the client with no server-side HTML.
 */
public interface TraceHighlightService {

    /**
     * Computes the cells to highlight on a frame's table, keyed by A1 address.
     *
     * <p>For a spreadsheet the current line is returned; for a decision table the matched and unmatched
     * condition cells and the fired rule's result are returned.
     *
     * @param frame the stack frame to highlight
     * @return the highlighted cells, possibly empty
     */
    List<CellHighlight> computeHighlights(DebugFrame frame);
}
