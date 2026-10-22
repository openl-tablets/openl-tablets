package org.openl.studio.projects.service.merge;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.diff.CellConflict;
import org.openl.rules.xls.merge.diff.CellPart;
import org.openl.rules.xls.merge.diff.CellSource;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.TableCells;
import org.openl.studio.projects.model.merge.ConflictCellView;
import org.openl.studio.projects.model.merge.ConflictTableMerge;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.service.tables.read.RawTableCells;
import org.openl.studio.projects.service.tables.read.RawTableStyleDictionary;

/**
 * Reads a conflicting table merged cell by cell the way the merge writes it: each cell of the merged table takes its
 * value, its style and its comment from the cells of the versions the merge takes them from, and the merged cells of
 * the merged table.
 *
 * <p>A conflicting cell shows the value and the comment of the base version until a version is decided for it. Each
 * conflict tells the cell as it is with either version as well. The cells one branch alone changed are named, as the
 * versions name the cells their branches changed.
 */
final class MergedTableReader {

    private final TableCells table;
    /** The areas of each version the merged table takes something from. */
    private final Map<Resolution, List<Read>> reads = new EnumMap<>(Resolution.class);
    /** The styles the cells of the merged table name, each once. */
    private final RawTableStyleDictionary styles = new RawTableStyleDictionary();
    /** The merged cells of the merged table, by their first cell. */
    private final Map<CellAddress, CellRangeAddress> regions = new HashMap<>();
    /** The cells merged cells cover, other than their first cells. */
    private final Set<CellAddress> covered = new HashSet<>();

    private MergedTableReader(TableCells table) {
        this.table = table;
        for (var region : table.regions()) {
            var first = new CellAddress(region.getFirstRow(), region.getFirstColumn());
            regions.put(first, region);
            for (var address : region) {
                if (!address.equals(first)) {
                    covered.add(address);
                }
            }
        }
    }

    /**
     * Reads a table merged cell by cell.
     *
     * @param table   the merged table
     * @param maxRows the most rows of the merged table read
     * @param reader  reads an area of the sheet in a version
     */
    static ConflictTableMerge read(TableCells table,
                                   int maxRows,
                                   BiFunction<Resolution, CellRangeAddress, RawTableCells> reader) {
        return new MergedTableReader(table).read(maxRows, reader);
    }

    private ConflictTableMerge read(int maxRows, BiFunction<Resolution, CellRangeAddress, RawTableCells> reader) {
        var place = table.place();
        var rows = place.getLastRow() - place.getFirstRow() + 1;
        var shown = new CellRangeAddress(place.getFirstRow(),
                place.getFirstRow() + Math.min(rows, maxRows) - 1,
                place.getFirstColumn(),
                place.getLastColumn());
        readVersions(shown, reader);
        var conflicts = new HashMap<CellAddress, CellConflict>();
        table.conflicts().forEach(conflict -> conflicts.put(conflict.cell(), conflict));
        var cells = new ArrayList<List<RawTableCell>>();
        for (var row = shown.getFirstRow(); row <= shown.getLastRow(); row++) {
            var line = new ArrayList<RawTableCell>();
            for (var column = shown.getFirstColumn(); column <= shown.getLastColumn(); column++) {
                var address = new CellAddress(row, column);
                var conflict = conflicts.get(address);
                line.add(conflict == null ? cellOf(address, sourceOf(address)) : undecidedOf(conflict));
            }
            cells.add(line);
        }
        var views = table.conflicts()
                .stream()
                .map(conflict -> new ConflictCellView(conflict.cell().getRow() - place.getFirstRow(),
                        conflict.cell().getColumn() - place.getFirstColumn(),
                        conflict.cell().formatAsString(),
                        conflict.our().formatAsString(),
                        conflict.their().formatAsString(),
                        cellOf(conflict.cell(), conflict.ourSource()),
                        cellOf(conflict.cell(), conflict.theirSource())))
                .toList();
        var changed = table.changed()
                .stream()
                .filter(address -> shown.isInRange(address.getRow(), address.getColumn()))
                .map(CellAddress::formatAsString)
                .toList();
        return new ConflictTableMerge(place.formatAsString(),
                cells,
                styles.styles(),
                rows,
                changed,
                views);
    }

    private CellSource sourceOf(CellAddress address) {
        return Objects.requireNonNull(table.cells().get(address), address::formatAsString);
    }

    /**
     * A conflicting cell no version is decided for: it shows the value and the comment of the base version where both
     * versions changed them differently, those parts their version gives when it is decided and your version does
     * not.
     */
    private RawTableCell undecidedOf(CellConflict conflict) {
        var parts = new HashSet<>(conflict.theirSource().parts());
        parts.removeAll(conflict.ourSource().parts());
        var base = find(Resolution.BASE, conflict.base());
        var source = conflict.ourSource();
        return cellOf(conflict.cell(),
                parts.contains(CellPart.VALUE) ? base : partOf(source, CellPart.VALUE),
                partOf(source, CellPart.STYLE),
                parts.contains(CellPart.COMMENT) ? base : partOf(source, CellPart.COMMENT));
    }

    /**
     * Reads, from each version, the area of the cells the shown cells and the conflicts among them take something
     * from. A conflict past the rows shown has the cells it takes something from read on their own, so that it costs
     * no reading of the rows between.
     */
    private void readVersions(CellRangeAddress shown,
                              BiFunction<Resolution, CellRangeAddress, RawTableCells> reader) {
        var areas = new EnumMap<Resolution, CellRangeAddress>(Resolution.class);
        for (var address : shown) {
            addSources(areas, sourceOf(address));
        }
        var apart = new ArrayList<Map<Resolution, CellRangeAddress>>();
        for (var conflict : table.conflicts()) {
            var into = shown.isInRange(conflict.cell())
                    ? areas
                    : new EnumMap<Resolution, CellRangeAddress>(Resolution.class);
            addSources(into, conflict.ourSource());
            addSources(into, conflict.theirSource());
            add(into, Resolution.BASE, conflict.base());
            if (into != areas) {
                apart.add(into);
            }
        }
        readAreas(areas, reader);
        apart.forEach(conflictAreas -> readAreas(conflictAreas, reader));
    }

    private void readAreas(Map<Resolution, CellRangeAddress> areas,
                           BiFunction<Resolution, CellRangeAddress, RawTableCells> reader) {
        areas.forEach((revision, area) -> {
            var read = reader.apply(revision, area);
            reads.computeIfAbsent(revision, unused -> new ArrayList<>())
                    .add(new Read(area, read.source(), Objects.requireNonNullElse(read.styles(), List.of())));
        });
    }

    private static void addSources(Map<Resolution, CellRangeAddress> areas, CellSource source) {
        add(areas, Resolution.OURS, source.our());
        add(areas, Resolution.BASE, source.base());
        if (!source.parts().isEmpty()) {
            add(areas, Resolution.THEIRS, source.their());
        }
    }

    private static void add(Map<Resolution, CellRangeAddress> areas,
                            Resolution revision,
                            @Nullable CellAddress address) {
        if (address != null) {
            var cell = new CellRangeAddress(address.getRow(), address.getRow(), address.getColumn(),
                    address.getColumn());
            areas.merge(revision, cell, (area, other) -> new CellRangeAddress(
                    Math.min(area.getFirstRow(), other.getFirstRow()),
                    Math.max(area.getLastRow(), other.getLastRow()),
                    Math.min(area.getFirstColumn(), other.getFirstColumn()),
                    Math.max(area.getLastColumn(), other.getLastColumn())));
        }
    }

    /**
     * A cell of the merged table as the merge writes it.
     */
    private RawTableCell cellOf(CellAddress address, CellSource source) {
        return cellOf(address,
                partOf(source, CellPart.VALUE),
                partOf(source, CellPart.STYLE),
                partOf(source, CellPart.COMMENT));
    }

    /**
     * The cell of a version a merged cell takes a part from: a cell of the base version a cell is a copy of, a cell
     * of their version for a part taken from it, a cell of your version otherwise.
     *
     * @return the cell, or {@code null} when the merged cell takes the part from none, which leaves it without it
     */
    private @Nullable Found partOf(CellSource source, CellPart part) {
        if (source.base() != null) {
            return find(Resolution.BASE, source.base());
        }
        return source.parts().contains(part)
                ? find(Resolution.THEIRS, source.their())
                : find(Resolution.OURS, source.our());
    }

    /**
     * A cell of the merged table: its value, its style and its comment each from the cell given, and how far it
     * reaches in the merged table.
     */
    private RawTableCell cellOf(CellAddress address,
                                @Nullable Found value,
                                @Nullable Found style,
                                @Nullable Found comment) {
        if (covered.contains(address)) {
            return RawTableCell.COVERED_CELL;
        }
        var region = regions.get(address);
        return RawTableCell.builder()
                .cell(address.formatAsString())
                .value(value == null ? null : value.cell().value())
                .formula(value == null ? null : value.cell().formula())
                .runs(value == null ? null : runsOf(value))
                .type(value == null ? null : value.cell().type())
                .comment(comment == null ? null : comment.cell().comment())
                .style(style == null ? null : styleOf(style.read(), style.cell().style()))
                .colspan(region == null ? null : region.getLastColumn() - region.getFirstColumn() + 1)
                .rowspan(region == null ? null : region.getLastRow() - region.getFirstRow() + 1)
                .build();
    }

    private @Nullable List<RawTableTextRun> runsOf(Found value) {
        var runs = value.cell().runs();
        return runs == null ? null : runs.stream()
                .map(run -> new RawTableTextRun(run.to(), styleOf(value.read(), run.style())))
                .toList();
    }

    /**
     * The index a style of a read is named by among the styles of the merged table.
     */
    private @Nullable Integer styleOf(Read read, @Nullable Integer index) {
        if (index == null || index < 0 || index >= read.styles().size()) {
            return null;
        }
        return styles.numberOf(read.styles().get(index));
    }

    private @Nullable Found find(Resolution revision, @Nullable CellAddress address) {
        if (address == null) {
            return null;
        }
        return reads.getOrDefault(revision, List.of())
                .stream()
                .filter(read -> read.area().isInRange(address))
                .findFirst()
                .map(read -> read.find(address))
                .orElse(null);
    }

    /** The cells of an area of a version, and the styles they name. */
    private record Read(CellRangeAddress area, List<List<RawTableCell>> cells, List<RawTableCellStyle> styles) {

        /**
         * A cell of the area.
         *
         * @return the cell, or {@code null} when the read has not got it
         */
        @Nullable Found find(CellAddress address) {
            var row = address.getRow() - area.getFirstRow();
            var column = address.getColumn() - area.getFirstColumn();
            if (row >= cells.size() || column >= cells.get(row).size()) {
                return null;
            }
            return new Found(cells.get(row).get(column), this);
        }
    }

    /** A cell of a version, with the read that found it. */
    private record Found(RawTableCell cell, Read read) {
    }
}
