package org.openl.studio.compare.service;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.xls.merge.compare.ChangeStatus;
import org.openl.rules.xls.merge.compare.LineChange;
import org.openl.rules.xls.merge.compare.SheetComparison;
import org.openl.rules.xls.merge.compare.TableComparison;
import org.openl.rules.xls.merge.compare.WorkbookComparison;
import org.openl.studio.compare.model.ComparisonLineView;
import org.openl.studio.compare.model.ComparisonNodeStatus;
import org.openl.studio.compare.model.ComparisonNodeType;
import org.openl.studio.compare.model.ComparisonNodeView;
import org.openl.studio.compare.model.ComparisonSideView;
import org.openl.studio.compare.model.ComparisonTableView;
import org.openl.studio.compare.model.ComparisonView;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Turns what the comparison found into what the API reports.
 *
 * <p>The comparison describes the two files sheet by sheet and table by table. A table is read from
 * the workbooks the comparison holds open, only when it is asked for.
 *
 * <p>An element is addressed by its position in the tree: a sheet by its index, a table by the
 * index of its sheet and its own, so the two sides of a table are found again without an index of
 * ids being kept alongside the comparison.
 */
@Component
@RequiredArgsConstructor
public class ComparisonMapper {

    private final RawTableReader tableReader;

    /**
     * Describes what the two compared files hold, sheet by sheet.
     *
     * @param id         identifier of the comparison
     * @param comparison what the comparison found
     * @return the comparison as the API reports it
     */
    public ComparisonView toView(String id, WorkbookComparison comparison) {
        var sheets = new ArrayList<ComparisonNodeView>();
        var compared = comparison.getSheets();
        for (var index = 0; index < compared.size(); index++) {
            sheets.add(toSheet(String.valueOf(index), compared.get(index)));
        }
        return ComparisonView.builder()
                .id(id)
                .identical(compared.stream().allMatch(sheet -> sheet.status() == ChangeStatus.EQUAL))
                .sheets(sheets)
                .build();
    }

    /**
     * Reads a compared table as it stands in each of the two files.
     *
     * @param comparison what the comparison found
     * @param tableId    identifier of the table within the comparison
     * @return the two sides of the table, or null when the comparison holds no such table
     */
    public @Nullable ComparisonTableView toTable(WorkbookComparison comparison, String tableId) {
        var position = tableId.split("-", -1);
        if (position.length != 2) {
            return null;
        }
        var sheet = at(comparison.getSheets(), position[0]);
        var table = sheet == null ? null : at(sheet.tables(), position[1]);
        if (table == null) {
            return null;
        }
        var firstSheet = comparison.firstSheet(sheet.name());
        var secondSheet = comparison.secondSheet(sheet.name());
        return ComparisonTableView.builder()
                .id(tableId)
                .name(table.name())
                .status(statusOf(table.status()))
                .moved(table.moved())
                .formattingChanged(table.formattingChanged())
                .firstRange(rangeOf(table.first()))
                .secondRange(rangeOf(table.second()))
                .first(toSide(firstSheet, table.first(), table.firstChanged()))
                .second(toSide(secondSheet, table.second(), table.secondChanged()))
                .rows(toLines(table.rows()))
                .columns(toLines(table.columns()))
                .build();
    }

    private static <T> @Nullable T at(List<T> elements, String index) {
        try {
            var at = Integer.parseInt(index);
            return at >= 0 && at < elements.size() ? elements.get(at) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ComparisonNodeView toSheet(String id, SheetComparison sheet) {
        var tables = new ArrayList<ComparisonNodeView>();
        var compared = sheet.tables();
        for (var index = 0; index < compared.size(); index++) {
            tables.add(toTableNode(id + "-" + index, compared.get(index)));
        }
        return ComparisonNodeView.builder()
                .id(id)
                .name(sheet.name())
                .type(ComparisonNodeType.SHEET)
                .status(statusOf(sheet.status()))
                .children(tables)
                .build();
    }

    private static ComparisonNodeView toTableNode(String id, TableComparison table) {
        return ComparisonNodeView.builder()
                .id(id)
                .name(table.name())
                .type(ComparisonNodeType.TABLE)
                .status(statusOf(table.status()))
                .moved(table.moved())
                .firstRange(rangeOf(table.first()))
                .secondRange(rangeOf(table.second()))
                .children(List.of())
                .build();
    }

    private static @Nullable String rangeOf(@Nullable CellRangeAddress place) {
        return place == null ? null : place.formatAsString();
    }

    /**
     * One side of a table, or null when that file does not hold the table at all.
     *
     * @param changed the cells of this side that read differently in the other file
     */
    private @Nullable ComparisonSideView toSide(@Nullable Sheet sheet,
                                                @Nullable CellRangeAddress place,
                                                List<CellAddress> changed) {
        if (sheet == null || place == null) {
            return null;
        }
        var cells = tableReader.cellsOf(sheet).apply(place);
        return ComparisonSideView.builder()
                .source(cells.source())
                .styles(cells.styles())
                .changedCells(changed.stream().map(CellAddress::formatAsString).toList())
                .build();
    }

    private static List<ComparisonLineView> toLines(List<LineChange> lines) {
        return lines.stream()
                .map(line -> new ComparisonLineView(indexOf(line.first()),
                        indexOf(line.second()),
                        statusOf(line.status())))
                .toList();
    }

    private static @Nullable Integer indexOf(int line) {
        return line == LineChange.NONE ? null : line;
    }

    private static ComparisonNodeStatus statusOf(ChangeStatus status) {
        return switch (status) {
            case EQUAL -> ComparisonNodeStatus.EQUAL;
            case CHANGED -> ComparisonNodeStatus.CHANGED;
            case MOVED -> ComparisonNodeStatus.MOVED;
            case ADDED -> ComparisonNodeStatus.ADDED;
            case REMOVED -> ComparisonNodeStatus.REMOVED;
        };
    }
}
