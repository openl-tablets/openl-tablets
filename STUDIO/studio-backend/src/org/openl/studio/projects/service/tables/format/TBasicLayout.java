package org.openl.studio.projects.service.tables.format;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.EnumUtils;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.tbasic.TBasicSpecificationKey;
import org.openl.studio.projects.model.tables.RawTableRole;

/**
 * Decides the role of each cell of the body of a TBasic table, an algorithm written in steps.
 *
 * <p>The first row of the body names the columns by their ids, such as {@code label}, {@code operation} or
 * {@code condition}: code the compiler reads. The row under it titles what the table takes. Every row after them is
 * a step. The label of a step is its key; its operation, its condition and what it runs are values holding code, and
 * its description, as a column the compiler knows no id of, a value.
 *
 * <p>A step whose operation is {@code SUB} or {@code FUNCTION} starts a subroutine: its row is a section. A step whose
 * operation is {@code RETURN} returns: its row is the result. The indent of an operation tells the compiler its level,
 * and formatting never changes it.
 *
 * <p>The compiled algorithm keeps the operations it runs, not where they stand, so the columns and the steps are read
 * from the sheet as the compiler reads them: the ids in the first row, the operations in the rows of the sheet from
 * the third row of the body on. A table whose steps the compiler could not build is read from the sheet the same
 * way.
 */
final class TBasicLayout {

    /** The row of the body the steps start at, under the ids and the titles of the columns. */
    private static final int STEPS = 2;

    private TBasicLayout() {
    }

    /**
     * The places of the body of a TBasic table the compiler built the steps of, read as it is written.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed layOut(TableBody body) {
        return new BodyLayout.Placed(body.rows(), Places.of(body));
    }

    /** A column of a TBasic table, by the id the compiler names it by, as {@code AlgorithmBuilder} reads it. */
    private enum Column {
        LABEL,
        DESCRIPTION,
        OPERATION,
        CONDITION,
        ACTION,
        BEFORE,
        AFTER,
        /** A column named nothing, or by an id the compiler does not know. */
        OTHER;

        /** The column an id names, in any case. */
        static Column of(String id) {
            return EnumUtils.getEnum(Column.class, id.toUpperCase(Locale.ROOT), OTHER);
        }
    }

    /**
     * What the places of one body are.
     *
     * @param columns  each column of the body, by its id
     * @param overlays the role of each step that starts a subroutine or returns, by the row of the sheet
     */
    private record Places(List<Column> columns, Map<Integer, RawTableRole> overlays)
            implements TableRegionLayout.PlaceRoles {

        private static final Place CODE = Place.of(RawTableRole.CODE);
        private static final Place TITLE = Place.of(RawTableRole.INPUT_TITLE);
        private static final Place LABEL = Place.of(RawTableRole.KEY);
        private static final Place CONDITION = Place.code(RawTableRole.VALUE);
        private static final Place ACTION = Place.code(RawTableRole.VALUE);
        private static final Place OPERATION = Place.code(RawTableRole.VALUE);
        private static final Place TEXT = Place.of(RawTableRole.VALUE);

        static Places of(TableBody body) {
            var columns = TableRegionLayout.idsOf(body.rows()).stream().map(Column::of).toList();
            return new Places(columns, overlaysOf(body.rows(), columns.indexOf(Column.OPERATION)));
        }

        /** What a cell of the body is, by the place it stands in. */
        @Override
        public Place at(ICell cell, int column, int row) {
            if (row == 0) {
                return CODE;
            }
            var named = columns.get(column);
            if (row == 1) {
                return TITLE;
            }
            var overlay = overlays.get(cell.getAbsoluteRow());
            var step = stepRole(named);
            return overlay == null ? step : step.as(overlay);
        }

        /** What a cell of a step is, by the column it stands in: its label is its key, the rest is code it runs. */
        private static Place stepRole(Column column) {
            return switch (column) {
                case LABEL -> LABEL;
                case CONDITION -> CONDITION;
                case ACTION, BEFORE, AFTER -> ACTION;
                case OPERATION -> OPERATION;
                case DESCRIPTION, OTHER -> TEXT;
            };
        }

        /**
         * The role of each step that starts a subroutine or returns, by the row of the sheet it stands in.
         *
         * <p>The compiler reads a step in each row of the sheet, its operation at the place of its id, and matches the
         * operation to its keywords in any case.
         *
         * @param operation the column of the operations, or {@code -1} for a table that names none
         */
        private static Map<Integer, RawTableRole> overlaysOf(ILogicalTable body, int operation) {
            var overlays = new HashMap<Integer, RawTableRole>();
            if (operation < 0 || body.getHeight() <= STEPS) {
                return overlays;
            }
            var steps = body.getRows(STEPS).getSource();
            for (var row = 0; row < steps.getHeight(); row++) {
                var cell = steps.getCell(operation, row);
                Optional.ofNullable(overlayOf(cell.getStringValue()))
                        .ifPresent(overlay -> overlays.put(cell.getAbsoluteRow(), overlay));
            }
            return overlays;
        }

        /** The role of a step by its operation: a section for a subroutine, the result for a return. */
        private static @Nullable RawTableRole overlayOf(@Nullable String operation) {
            var keyword = EnumUtils.getEnum(TBasicSpecificationKey.class,
                    Objects.requireNonNullElse(operation, "").toUpperCase(Locale.ROOT));
            return switch (keyword) {
                case SUB, FUNCTION -> RawTableRole.SECTION;
                case RETURN -> RawTableRole.RESULT;
                case null, default -> null;
            };
        }
    }
}
