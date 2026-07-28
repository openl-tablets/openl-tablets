package org.openl.studio.projects.service.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.xls.XlsUtil;
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
     * Highlights for a decision-table frame: matched (green) and unmatched (red) condition cells, and the
     * fired rule — its whole row, result cell included — accented as the returned rule. Precedence is fired
     * rule over matched over unmatched, so a cell in more than one region keeps the strongest state.
     */
    private static List<CellHighlight> dtHighlights(DebugFrame frame) {
        var byCell = new LinkedHashMap<String, HighlightState>();
        DtRegions regions = dtRegions(frame);
        putAll(byCell, regions.unmatched(), HighlightState.CONDITION_FALSE);
        putAll(byCell, regions.matched(), HighlightState.CONDITION_TRUE);
        putAll(byCell, firedRuleResultRegions(frame), HighlightState.RESULT);
        return byCell.entrySet().stream().map(e -> new CellHighlight(e.getKey(), e.getValue())).toList();
    }

    private static void putAll(Map<String, HighlightState> byCell, List<IGridRegion> regions, HighlightState state) {
        for (IGridRegion region : regions) {
            for (String address : cellAddresses(region)) {
                byCell.put(address, state);
            }
        }
    }

    /** The A1 address of a region's top-left cell, matching the raw table's cell addresses. */
    private static String cellAddress(IGridRegion region) {
        return XlsUtil.xlsCellPresentation(region.getLeft(), region.getTop());
    }

    /**
     * Every A1 cell address a region spans. A decision-table condition can cover several cells and a rule is a
     * whole row (or column), so the highlight paints the entire region, not just its corner — for any orientation.
     */
    private static List<String> cellAddresses(IGridRegion region) {
        List<String> addresses = new ArrayList<>();
        for (int column = region.getLeft(); column <= region.getRight(); column++) {
            for (int row = region.getTop(); row <= region.getBottom(); row++) {
                addresses.add(XlsUtil.xlsCellPresentation(column, row));
            }
        }
        return addresses;
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
        var ref = frame.getLocation().ref();
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
        var matched = new ArrayList<IGridRegion>();
        var unmatched = new ArrayList<IGridRegion>();
        for (ConditionCheck check : frame.getConditionChecks()) {
            if (!(check.condition() instanceof IBaseCondition condition)) {
                continue;
            }
            List<IGridRegion> target = check.successful() ? matched : unmatched;
            for (int rule : check.rules()) {
                var valueCell = condition.getValueCell(rule);
                if (valueCell != null) {
                    target.addAll(GridTableUtils.getGridRegions(valueCell));
                }
            }
        }
        return new DtRegions(matched, unmatched);
    }

    private static List<IGridRegion> firedRuleResultRegions(DebugFrame frame) {
        if (frame.getSource() instanceof IDecisionTable decisionTable) {
            var regions = new ArrayList<IGridRegion>();
            for (int rule : frame.getFiredRules()) {
                regions.addAll(GridTableUtils.getGridRegions(decisionTable.getRuleTable(rule)));
            }
            return regions;
        }
        return List.of();
    }

    private record DtRegions(List<IGridRegion> matched, List<IGridRegion> unmatched) {
    }
}
