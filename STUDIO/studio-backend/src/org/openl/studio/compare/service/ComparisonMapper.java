package org.openl.studio.compare.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.xls.merge.XlsWorkbookComparator;
import org.openl.rules.xls.merge.compare.ChangeStatus;
import org.openl.rules.xls.merge.compare.ConflictComparison;
import org.openl.rules.xls.merge.compare.ConflictLine;
import org.openl.rules.xls.merge.compare.ConflictSheet;
import org.openl.rules.xls.merge.compare.ConflictTable;
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
import org.openl.studio.compare.model.ConflictComparisonLineView;
import org.openl.studio.compare.model.ConflictComparisonTableView;
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
        var compared = comparison.getSheets();
        return ComparisonView.builder()
                .id(id)
                .identical(compared.stream().allMatch(sheet -> sheet.status() == ChangeStatus.EQUAL))
                .sheets(numbered(compared, "", ComparisonMapper::toSheet))
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
        var found = locate(comparison.getSheets(), SheetComparison::tables, tableId);
        if (found == null) {
            return null;
        }
        var table = found.table();
        var firstSheet = comparison.firstSheet(found.sheet().name());
        var secondSheet = comparison.secondSheet(found.sheet().name());
        return ComparisonTableView.builder()
                .id(tableId)
                .name(table.name())
                .status(statusOf(table.status()))
                .moved(table.moved())
                .formattingChanged(table.formattingChanged())
                .firstRange(rangeOf(table.first()))
                .secondRange(rangeOf(table.second()))
                .first(toSide(firstSheet, table.first(), table.firstChanged(), null))
                .second(toSide(secondSheet, table.second(), table.secondChanged(), null))
                .rows(toLines(table.rows()))
                .columns(toLines(table.columns()))
                .build();
    }

    /**
     * Describes what the base version and the two versions of a conflict hold, sheet by sheet: each element with
     * what each version did to it, and what both did together.
     *
     * @param id         identifier of the comparison
     * @param comparison what the comparison found
     * @return the comparison as the API reports it
     */
    public ComparisonView toConflictView(String id, ConflictComparison comparison) {
        var compared = comparison.getSheets();
        return ComparisonView.builder()
                .id(id)
                .identical(compared.stream().allMatch(sheet -> together(sheet.yoursStatus(), sheet.theirsStatus())
                        == ChangeStatus.EQUAL))
                .sheets(numbered(compared, "", ComparisonMapper::toConflictSheet))
                .build();
    }

    /**
     * Reads a table of a conflict as the base and the two versions hold it.
     *
     * @param comparison what the comparison found
     * @param tableId    identifier of the table within the comparison
     * @return the three versions of the table, or null when the comparison holds no such table
     */
    public @Nullable ConflictComparisonTableView toConflictTable(ConflictComparison comparison, String tableId) {
        var found = locate(comparison.getSheets(), ConflictSheet::tables, tableId);
        if (found == null) {
            return null;
        }
        var table = found.table();
        var name = found.sheet().name();
        return ConflictComparisonTableView.builder()
                .id(tableId)
                .name(table.name())
                .status(statusOf(together(table.yoursStatus(), table.theirsStatus())))
                .yoursStatus(statusOf(table.yoursStatus()))
                .theirsStatus(statusOf(table.theirsStatus()))
                .baseRange(rangeOf(table.base()))
                .yoursRange(rangeOf(table.yours()))
                .theirsRange(rangeOf(table.theirs()))
                .base(toSide(comparison.baseSheet(name), table.base(), List.of(), null))
                .yours(toSide(comparison.yoursSheet(name), table.yours(), table.yoursChanged(), table.yoursConflicts()))
                .theirs(toSide(comparison.theirsSheet(name), table.theirs(), table.theirsChanged(),
                        table.theirsConflicts()))
                .rows(toConflictLines(table.rows()))
                .columns(toConflictLines(table.columns()))
                .build();
    }

    /** What two versions did to an element together: what either did when the other did nothing or the same. */
    private static ChangeStatus together(ChangeStatus yours, ChangeStatus theirs) {
        if (yours == theirs || theirs == ChangeStatus.EQUAL) {
            return yours;
        }
        return yours == ChangeStatus.EQUAL ? theirs : ChangeStatus.CHANGED;
    }

    private static ComparisonNodeView toConflictSheet(String id, ConflictSheet sheet) {
        return ComparisonNodeView.builder()
                .id(id)
                .name(sheet.name())
                .type(ComparisonNodeType.SHEET)
                .status(statusOf(together(sheet.yoursStatus(), sheet.theirsStatus())))
                .yours(statusOf(sheet.yoursStatus()))
                .theirs(statusOf(sheet.theirsStatus()))
                .children(numbered(sheet.tables(), id + "-", ComparisonMapper::toConflictTableNode))
                .build();
    }

    private static ComparisonNodeView toConflictTableNode(String id, ConflictTable table) {
        return ComparisonNodeView.builder()
                .id(id)
                .name(table.name())
                .type(ComparisonNodeType.TABLE)
                .status(statusOf(together(table.yoursStatus(), table.theirsStatus())))
                .yours(statusOf(table.yoursStatus()))
                .theirs(statusOf(table.theirsStatus()))
                .moved(false)
                .other(otherOf(table.name()))
                .conflict(table.yoursConflicts().isEmpty() ? null : Boolean.TRUE)
                .children(List.of())
                .build();
    }

    /** Marks the cells outside OpenL tables, such as notes or formulas beside them; an OpenL table is not marked. */
    private static @Nullable Boolean otherOf(String name) {
        return XlsWorkbookComparator.isOpenLTable(name) ? null : Boolean.TRUE;
    }

    private static List<ConflictComparisonLineView> toConflictLines(List<ConflictLine> lines) {
        return lines.stream()
                .map(line -> new ConflictComparisonLineView(indexOf(line.base()),
                        indexOf(line.yours()),
                        indexOf(line.theirs()),
                        statusOf(line.yoursStatus()),
                        statusOf(line.theirsStatus())))
                .toList();
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
        return ComparisonNodeView.builder()
                .id(id)
                .name(sheet.name())
                .type(ComparisonNodeType.SHEET)
                .status(statusOf(sheet.status()))
                .children(numbered(sheet.tables(), id + "-", ComparisonMapper::toTableNode))
                .build();
    }

    /**
     * The nodes of elements, each identified by its place among them after the given prefix: {@code 0}, {@code 1}, and
     * so on for the sheets, {@code 0-0}, {@code 0-1} for the tables of the first sheet.
     */
    private static <T> List<ComparisonNodeView> numbered(List<T> elements,
                                                         String prefix,
                                                         BiFunction<String, T, ComparisonNodeView> node) {
        return IntStream.range(0, elements.size())
                .mapToObj(index -> node.apply(prefix + index, elements.get(index)))
                .toList();
    }

    /** A table found by its identifier, with the sheet it stands on. */
    private record Located<S, T>(S sheet, T table) {
    }

    /**
     * Finds a table by its identifier: the place of its sheet and its place on the sheet, such as {@code 0-1}.
     *
     * @return the table and its sheet, or null when no table stands there
     */
    private static <S, T> @Nullable Located<S, T> locate(List<S> sheets, Function<S, List<T>> tablesOf, String tableId) {
        var position = tableId.split("-", -1);
        if (position.length != 2) {
            return null;
        }
        var sheet = at(sheets, position[0]);
        var table = sheet == null ? null : at(tablesOf.apply(sheet), position[1]);
        return table == null ? null : new Located<>(sheet, table);
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
                .other(otherOf(table.name()))
                .children(List.of())
                .build();
    }

    private static @Nullable String rangeOf(@Nullable CellRangeAddress place) {
        return place == null ? null : place.formatAsString();
    }

    /**
     * One side of a table, or null when that file does not hold the table at all.
     *
     * @param changed   the cells of this side that read differently in the other file
     * @param conflicts the cells both versions of a conflict changed differently, or null outside a conflict
     */
    private @Nullable ComparisonSideView toSide(@Nullable Sheet sheet,
                                                @Nullable CellRangeAddress place,
                                                List<CellAddress> changed,
                                                @Nullable List<CellAddress> conflicts) {
        if (sheet == null || place == null) {
            return null;
        }
        var cells = tableReader.cellsOf(sheet).apply(place);
        return ComparisonSideView.builder()
                .source(cells.source())
                .styles(cells.styles())
                .changedCells(changed.stream().map(CellAddress::formatAsString).toList())
                .conflictCells(conflicts == null ? null : conflicts.stream().map(CellAddress::formatAsString).toList())
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
