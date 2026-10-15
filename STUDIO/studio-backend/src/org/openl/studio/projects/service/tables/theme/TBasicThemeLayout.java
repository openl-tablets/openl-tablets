package org.openl.studio.projects.service.tables.theme;

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
 * Decides what each cell of the body of a TBasic table is, an algorithm written in steps: its role, and the look a
 * theme gives it.
 *
 * <p>The ids of the columns are code the compiler reads, and the titles under them titles of what the table takes.
 * The label of a step is its key; its operation, its condition and what it runs are values holding code, and its
 * description a value. A step starting a subroutine heads a section, and a step that returns is the result.
 *
 * <p>The first row of the body names the columns by their ids, such as {@code label}, {@code operation} or
 * {@code condition}, and gets the code look. The row under it titles the columns and gets the title look, and the
 * title of the column of labels gets the step title look over it. Every row after them is a step. Its label gets the
 * step look and its condition the condition look. What it runs gets the value look: its action, and what it runs
 * before and after it. Its description and its operation keep the look every cell starts from, as a column the
 * compiler knows no id of does. The last row gets the last-row look over its own.
 *
 * <p>A step whose operation is {@code SUB} or {@code FUNCTION} starts a subroutine: every cell of its row gets the
 * section look over its own. A step whose operation is {@code RETURN} returns: every cell of its row gets the result
 * look over its own. The indent of an operation tells the compiler its level, and the theme never changes it.
 *
 * <p>The compiled algorithm keeps the operations it runs, not where they stand, so the columns and the steps are read
 * from the sheet as the compiler reads them: the ids in the first row, the operations in the rows of the sheet from
 * the third row of the body on. A table whose steps the compiler could not build takes the look every cell starts
 * from for its body, see {@link CompiledReads}.
 */
final class TBasicThemeLayout {

    /** The row of the body the steps start at, under the ids and the titles of the columns. */
    private static final int STEPS = 2;

    private TBasicThemeLayout() {
    }

    /**
     * The places of the body of a TBasic table the compiler built the steps of, read as it is written.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed layOut(ThemedBody body) {
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
     * @param overlays the part laid over each step that starts a subroutine or returns, by the row of the sheet
     */
    private record Places(List<Column> columns, Map<Integer, Parts.Part> overlays)
            implements TableRegionLayout.PlaceLook {

        private static final Place CODE = Place.of(RawTableRole.CODE, Parts.BASE.with(Parts.Part.CODE));
        private static final Place TITLE = Place.of(RawTableRole.INPUT_TITLE, Parts.BASE.with(Parts.Part.TITLES));
        private static final Place LABEL_TITLE = TITLE.with(Parts.Part.STEP_TITLE);
        private static final Place LABEL = Place.of(RawTableRole.KEY, Parts.BASE.with(Parts.Part.STEPS));
        private static final Place CONDITION = Place.code(RawTableRole.VALUE, Parts.BASE.with(Parts.Part.CONDITION));
        private static final Place ACTION = Place.code(RawTableRole.VALUE, Parts.BASE.with(Parts.Part.VALUES));
        private static final Place OPERATION = Place.code(RawTableRole.VALUE, Parts.BASE);
        private static final Place TEXT = Place.of(RawTableRole.VALUE, Parts.BASE);

        static Places of(ThemedBody body) {
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
                return named == Column.LABEL ? LABEL_TITLE : TITLE;
            }
            var overlay = overlays.get(cell.getAbsoluteRow());
            var step = stepLook(named).with(overlay);
            if (overlay == Parts.Part.SECTIONS) {
                return step.as(RawTableRole.SECTION);
            }
            return overlay == Parts.Part.RESULT ? step.as(RawTableRole.RESULT) : step;
        }

        /** What a cell of a step is, by the column it stands in: its label is its key, the rest is code it runs. */
        private static Place stepLook(Column column) {
            return switch (column) {
                case LABEL -> LABEL;
                case CONDITION -> CONDITION;
                case ACTION, BEFORE, AFTER -> ACTION;
                case OPERATION -> OPERATION;
                case DESCRIPTION, OTHER -> TEXT;
            };
        }

        /**
         * The part laid over each step that starts a subroutine or returns, by the row of the sheet it stands in.
         *
         * <p>The compiler reads a step in each row of the sheet, its operation at the place of its id, and matches the
         * operation to its keywords in any case.
         *
         * @param operation the column of the operations, or {@code -1} for a table that names none
         */
        private static Map<Integer, Parts.Part> overlaysOf(ILogicalTable body, int operation) {
            var overlays = new HashMap<Integer, Parts.Part>();
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

        /**
         * The part laid over a step by its operation: the sections for a subroutine, the result for a return.
         */
        private static Parts.@Nullable Part overlayOf(@Nullable String operation) {
            var keyword = EnumUtils.getEnum(TBasicSpecificationKey.class,
                    Objects.requireNonNullElse(operation, "").toUpperCase(Locale.ROOT));
            return switch (keyword) {
                case SUB, FUNCTION -> Parts.Part.SECTIONS;
                case RETURN -> Parts.Part.RESULT;
                case null, default -> null;
            };
        }
    }
}
