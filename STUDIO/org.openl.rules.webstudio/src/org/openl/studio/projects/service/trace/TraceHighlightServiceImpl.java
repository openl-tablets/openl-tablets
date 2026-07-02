package org.openl.studio.projects.service.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.xls.XlsUtil;
import org.openl.rules.webstudio.web.trace.debug.ConditionCheck;
import org.openl.rules.webstudio.web.trace.debug.CurrentLocation;
import org.openl.rules.webstudio.web.trace.debug.DebugFrame;
import org.openl.studio.projects.model.trace.CellHighlight;
import org.openl.studio.projects.model.trace.HighlightState;

/**
 * Default {@link TraceHighlightService}. Resolves a frame's current line, evaluated conditions and fired
 * rule to absolute grid cells and returns them as an A1-keyed overlay for the client to paint.
 */
@Service
public class TraceHighlightServiceImpl implements TraceHighlightService {

    @Override
    public List<CellHighlight> computeHighlights(DebugFrame frame) {
        if (frame.getSource() instanceof IDecisionTable) {
            return dtHighlights(frame);
        }
        return currentStepRegions(frame).stream()
                .map(region -> new CellHighlight(cellAddress(region), HighlightState.CURRENT))
                .toList();
    }

    /**
     * Highlights for a decision-table frame: matched (green) and unmatched (red) condition cells and the
     * fired rule's result (blue). Precedence is result over matched over unmatched, so a cell in more than
     * one region keeps the strongest state.
     */
    private static List<CellHighlight> dtHighlights(DebugFrame frame) {
        Map<String, HighlightState> byCell = new LinkedHashMap<>();
        DtRegions regions = dtRegions(frame);
        putAll(byCell, regions.unmatched(), HighlightState.CONDITION_FALSE);
        putAll(byCell, regions.matched(), HighlightState.CONDITION_TRUE);
        putAll(byCell, firedRuleResultRegions(frame), HighlightState.RESULT);
        return byCell.entrySet().stream().map(e -> new CellHighlight(e.getKey(), e.getValue())).toList();
    }

    private static void putAll(Map<String, HighlightState> byCell, List<IGridRegion> regions, HighlightState state) {
        for (IGridRegion region : regions) {
            byCell.put(cellAddress(region), state);
        }
    }

    /** The A1 address of a region's top-left cell, matching the raw table's cell addresses. */
    private static String cellAddress(IGridRegion region) {
        return XlsUtil.xlsCellPresentation(region.getLeft(), region.getTop());
    }

    private static List<IGridRegion> currentStepRegions(DebugFrame frame) {
        if (frame.getCurrentStep() instanceof SpreadsheetCell cell) {
            return List.of(cell.getSourceCell().getAbsoluteRegion());
        }
        // Robust fallback: resolve the current spreadsheet cell from the location reference, since the
        // live cell reference can be cleared by intermediate trace events.
        IGridRegion region = currentSpreadsheetCellRegion(frame);
        return region == null ? List.of() : List.of(region);
    }

    private static @Nullable IGridRegion currentSpreadsheetCellRegion(DebugFrame frame) {
        if (!(frame.getSource() instanceof Spreadsheet spreadsheet) || frame.getLocation() == null) {
            return null;
        }
        String ref = frame.getLocation().ref();
        if (ref == null) {
            return null;
        }
        for (SpreadsheetCell[] row : spreadsheet.getCells()) {
            for (SpreadsheetCell cell : row) {
                if (cell != null && CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex()).equals(ref)) {
                    return cell.getSourceCell().getAbsoluteRegion();
                }
            }
        }
        return null;
    }

    /** The decision table's evaluated condition value cells, split into matched and unmatched. */
    private static DtRegions dtRegions(DebugFrame frame) {
        List<IGridRegion> matched = new ArrayList<>();
        List<IGridRegion> unmatched = new ArrayList<>();
        for (ConditionCheck check : frame.getConditionChecks()) {
            if (!(check.condition() instanceof IBaseCondition condition)) {
                continue;
            }
            List<IGridRegion> target = check.successful() ? matched : unmatched;
            for (int rule : check.rules()) {
                ILogicalTable valueCell = condition.getValueCell(rule);
                if (valueCell != null) {
                    target.addAll(GridTableUtils.getGridRegions(valueCell));
                }
            }
        }
        return new DtRegions(matched, unmatched);
    }

    private static List<IGridRegion> firedRuleResultRegions(DebugFrame frame) {
        if (frame.getCurrentStep() instanceof ActionInvoker invoker
                && frame.getSource() instanceof IDecisionTable decisionTable) {
            List<IGridRegion> regions = new ArrayList<>();
            for (int rule : invoker.getRules()) {
                regions.addAll(GridTableUtils.getGridRegions(decisionTable.getRuleTable(rule)));
            }
            return regions;
        }
        return List.of();
    }

    private record DtRegions(List<IGridRegion> matched, List<IGridRegion> unmatched) {
    }
}
