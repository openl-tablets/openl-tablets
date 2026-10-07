package org.openl.studio.compare.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjection;
import org.openl.rules.table.ICell;
import org.openl.studio.compare.model.ComparisonNodeStatus;
import org.openl.studio.compare.model.ComparisonNodeType;
import org.openl.studio.compare.model.ComparisonNodeView;
import org.openl.studio.compare.model.ComparisonPropertyChange;
import org.openl.studio.compare.model.ComparisonSideView;
import org.openl.studio.compare.model.ComparisonTableView;
import org.openl.studio.compare.model.ComparisonView;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Turns what the comparison engine found into what the API reports.
 *
 * <p>The engine describes the two files as one tree of sheets and tables, holding an element per
 * file at every position. The element of the second file carries the status, and the element of the
 * first one is there whenever that file holds the position at all.
 *
 * <p>An element is addressed by its position in the tree: a sheet by its index, a table by the
 * index of its sheet and its own, so the two sides of a table are found again without an index of
 * ids being kept alongside the comparison.
 */
@Component
@RequiredArgsConstructor
public class ComparisonMapper {

    /** The element of the second file; the first one is the original the second is compared against. */
    private static final int SECOND = 1;
    private static final int FIRST = 0;

    private final RawTableReader tableReader;

    /**
     * Describes what the two compared files hold, sheet by sheet.
     *
     * @param id   identifier of the comparison
     * @param root what the comparison engine found
     * @return the comparison as the API reports it
     */
    public ComparisonView toView(String id, DiffTreeNode root) {
        var sheets = new ArrayList<ComparisonNodeView>();
        var children = root.getChildren();
        for (var index = 0; index < children.size(); index++) {
            sheets.add(toSheet(String.valueOf(index), children.get(index)));
        }
        return ComparisonView.builder()
                .id(id)
                .identical(statusOf(root) == ComparisonNodeStatus.EQUAL)
                .sheets(sheets)
                .build();
    }

    /**
     * Reads a compared table as it stands in each of the two files.
     *
     * @param root    what the comparison engine found
     * @param tableId identifier of the table within the comparison
     * @return the two sides of the table, or null when the comparison holds no such table
     */
    public @Nullable ComparisonTableView toTable(DiffTreeNode root, String tableId) {
        var node = findTable(root, tableId);
        if (node == null) {
            return null;
        }
        return ComparisonTableView.builder()
                .id(tableId)
                .name(nameOf(node))
                .status(statusOf(node))
                .first(toSide(node.getElement(FIRST), node.getElement(SECOND)))
                .second(toSide(node.getElement(SECOND), node.getElement(FIRST)))
                .build();
    }

    private ComparisonNodeView toSheet(String id, DiffTreeNode node) {
        var tables = new ArrayList<ComparisonNodeView>();
        var children = node.getChildren();
        for (var index = 0; index < children.size(); index++) {
            tables.add(toTableNode(id + "-" + index, children.get(index)));
        }
        return ComparisonNodeView.builder()
                .id(id)
                .name(nameOf(node))
                .type(ComparisonNodeType.SHEET)
                .status(statusOf(node))
                .changes(List.of())
                .children(tables)
                .build();
    }

    private ComparisonNodeView toTableNode(String id, DiffTreeNode node) {
        return ComparisonNodeView.builder()
                .id(id)
                .name(nameOf(node))
                .type(ComparisonNodeType.TABLE)
                .status(statusOf(node))
                .changes(changesOf(node))
                .children(List.of())
                .build();
    }

    /**
     * The table of a comparison at the position the identifier names, in the form
     * {@code <sheet index>-<table index>}.
     */
    private static @Nullable DiffTreeNode findTable(DiffTreeNode root, String tableId) {
        var position = tableId.split("-", -1);
        if (position.length != 2) {
            return null;
        }
        var sheet = childAt(root, position[0]);
        return sheet == null ? null : childAt(sheet, position[1]);
    }

    private static @Nullable DiffTreeNode childAt(DiffTreeNode node, String index) {
        try {
            var children = node.getChildren();
            var at = Integer.parseInt(index);
            return at >= 0 && at < children.size() ? children.get(at) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * One side of a table, or null when that file does not hold the table at all. The cells that
     * differ are the ones the engine found on this side.
     */
    private @Nullable ComparisonSideView toSide(DiffElement element, DiffElement other) {
        if (!(element.getProjection() instanceof XlsProjection projection) || projection.getTable() == null) {
            return null;
        }
        // A table the other file does not hold at all is not marked cell by cell: every one of its
        // cells is new or gone, which the status of the table already says.
        var comparable = other.getProjection() != null;
        return ComparisonSideView.builder()
                .source(tableReader.readCells(projection.getTable(), true))
                .changedCells(comparable ? changedCellsOf(projection) : List.of())
                .build();
    }

    /** The addresses of the cells of this side that read differently in the other file. */
    private static List<String> changedCellsOf(XlsProjection projection) {
        if (projection.getDiffCells() == null) {
            return List.of();
        }
        return projection.getDiffCells().stream()
                .map(ICell::getUri)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** Properties of the table that read differently in the two files, such as its name or its size. */
    private static List<ComparisonPropertyChange> changesOf(DiffTreeNode node) {
        var first = node.getElement(FIRST).getProjection();
        var second = node.getElement(SECOND).getProjection();
        if (first == null || second == null || node.getElement(SECOND).isSelfEqual()) {
            return List.of();
        }
        var changes = new ArrayList<ComparisonPropertyChange>();
        for (var property : first.getProperties()) {
            var counterpart = second.getProperty(property.getName());
            if (counterpart != null && !Objects.equals(property.getRawValue(), counterpart.getRawValue())) {
                changes.add(new ComparisonPropertyChange(property.getName(),
                        asText(property.getRawValue()),
                        asText(counterpart.getRawValue())));
            }
        }
        return changes;
    }

    private static @Nullable String asText(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * The name of an element as the first file spells it, or as the second one does when the element
     * was added there.
     */
    private static String nameOf(DiffTreeNode node) {
        var status = node.getElement(SECOND).getDiffStatus();
        Projection projection = status == DiffStatus.ADDED
                ? node.getElement(SECOND).getProjection()
                : node.getElement(FIRST).getProjection();
        return projection == null ? "" : projection.getName();
    }

    private static ComparisonNodeStatus statusOf(DiffTreeNode node) {
        return switch (node.getElement(SECOND).getDiffStatus()) {
            case ADDED -> ComparisonNodeStatus.ADDED;
            case REMOVED -> ComparisonNodeStatus.REMOVED;
            case EQUALS -> ComparisonNodeStatus.EQUAL;
            default -> ComparisonNodeStatus.CHANGED;
        };
    }
}
