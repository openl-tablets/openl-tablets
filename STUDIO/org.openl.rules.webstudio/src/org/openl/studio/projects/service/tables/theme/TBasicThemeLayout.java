package org.openl.studio.projects.service.tables.theme;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.Builder;
import org.apache.commons.lang3.EnumUtils;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.tbasic.TBasicSpecificationKey;

/**
 * Decides which look a theme gives each cell of the body of a TBasic table: an algorithm written in steps.
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
 * <p>The columns and the steps are read as the compiler reads them: the ids in the first row, the operations in the
 * rows of the sheet from the third row of the body on. A table being edited is therefore themed before it is
 * compiled.
 */
final class TBasicThemeLayout {

    /** The row of the body the steps start at, under the ids and the titles of the columns. */
    private static final int STEPS = 2;

    private TBasicThemeLayout() {
    }

    /**
     * The places of the body of a TBasic table, read as it is written.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
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
     * The looks of the places of one body, each laid over the base once rather than once per cell.
     *
     * @param base       the look of a description, an operation and a column of no known id
     * @param code       the look of an id
     * @param title      the look of a column title
     * @param labelTitle the look of the title of the column of labels
     * @param label      the look of a label
     * @param condition  the look of the condition of a step
     * @param value      the look of what a step runs
     * @param columns    each column of the body, by its id
     * @param overlays   the look laid over each step that starts a subroutine or returns, by the row of the sheet
     */
    @Builder
    private record Places(ThemeStyle base,
                          ThemeStyle code,
                          ThemeStyle title,
                          ThemeStyle labelTitle,
                          ThemeStyle label,
                          ThemeStyle condition,
                          ThemeStyle value,
                          List<Column> columns,
                          Map<Integer, ThemeStyle> overlays) implements ThemeLayouts.PlaceLook {

        static Places of(ThemedBody body) {
            var base = body.base();
            var look = body.look();
            var columns = ThemeLayouts.idsOf(body.rows()).stream().map(Column::of).toList();
            return Places.builder()
                    .base(base)
                    .code(base.with(look.code()))
                    .title(base.with(look.titles()))
                    .labelTitle(base.with(look.titles()).with(look.stepTitle()))
                    .label(base.with(look.steps()))
                    .condition(base.with(look.condition()))
                    .value(base.with(look.values()))
                    .columns(columns)
                    .overlays(overlaysOf(body.rows(), columns.indexOf(Column.OPERATION), look))
                    .build();
        }

        /** The look of a cell of the body, by the place it stands in. */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            if (row == 0) {
                return code;
            }
            var named = columns.get(column);
            if (row == 1) {
                return named == Column.LABEL ? labelTitle : title;
            }
            return stepLook(named).with(overlays.get(cell.getAbsoluteRow()));
        }

        /** The look of a cell of a step, by the column it stands in. */
        private ThemeStyle stepLook(Column column) {
            return switch (column) {
                case LABEL -> label;
                case CONDITION -> condition;
                case ACTION, BEFORE, AFTER -> value;
                case DESCRIPTION, OPERATION, OTHER -> base;
            };
        }

        /**
         * The look laid over each step that starts a subroutine or returns, by the row of the sheet it stands in.
         *
         * <p>The compiler reads a step in each row of the sheet, its operation at the place of its id, and matches the
         * operation to its keywords in any case.
         *
         * @param operation the column of the operations, or {@code -1} for a table that names none
         */
        private static Map<Integer, ThemeStyle> overlaysOf(ILogicalTable body, int operation, TableTheme.Look look) {
            var overlays = new HashMap<Integer, ThemeStyle>();
            if (operation < 0 || body.getHeight() <= STEPS) {
                return overlays;
            }
            var steps = body.getRows(STEPS).getSource();
            for (var row = 0; row < steps.getHeight(); row++) {
                var cell = steps.getCell(operation, row);
                Optional.ofNullable(overlayOf(cell.getStringValue(), look))
                        .ifPresent(overlay -> overlays.put(cell.getAbsoluteRow(), overlay));
            }
            return overlays;
        }

        /**
         * The look laid over a step by its operation: the section look for a subroutine, the result look for a
         * return.
         */
        private static @Nullable ThemeStyle overlayOf(@Nullable String operation, TableTheme.Look look) {
            var keyword = EnumUtils.getEnum(TBasicSpecificationKey.class,
                    Objects.requireNonNullElse(operation, "").toUpperCase(Locale.ROOT));
            return switch (keyword) {
                case SUB, FUNCTION -> look.sections();
                case RETURN -> look.result();
                case null, default -> null;
            };
        }
    }
}
