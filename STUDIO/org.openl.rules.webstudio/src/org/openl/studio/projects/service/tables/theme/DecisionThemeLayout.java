package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;

/**
 * Decides which look a theme gives each cell of the body of a decision table: a Rules, a SimpleRules, a SmartRules, a
 * SimpleLookup or a SmartLookup table.
 *
 * <p>The table is read as the compiler reads it. A Rules table declares its columns in rows of code above their
 * titles: the kind of each column, such as {@code C1} or {@code RET1}, its expression and its parameters. A
 * SimpleRules, a SmartRules and a lookup have none: the compiler matches their titles to the parameters instead. The
 * titles and the values of a condition get the title and the value look, and so does the column a table names its
 * rules in. The titles and the values of a column the table returns or acts in get the return title and the return
 * look. A lookup also checks horizontal conditions, whose values stand across its top over the values it returns:
 * they get the horizontal look. A value of a condition merged over several rules makes them a group: the first of
 * them and the rule after the group get the group look over their own, which sets the group apart.
 *
 * <p>Where each part stands is taken from the compiled table: the lines of the sheet its code, its titles and its
 * horizontal conditions take, and the columns its conditions and what it returns take. Every line under them holds
 * rules, so the rules a table being edited gained are themed before it is compiled. A row or a column an edit
 * inserted or deleted since the table was compiled moves the parts after it, and each part is themed where the edit
 * moved it. A table that did not compile takes the base skin alone.
 *
 * <p>A table written the other way round, with a rule in each column, takes the looks with its rows and columns
 * swapped, and so are the lines of a look: a line it draws above a part is drawn on the left of the part, a line
 * below it on the right. The style every cell starts from keeps its lines where it names them.
 */
final class DecisionThemeLayout {

    private DecisionThemeLayout() {
    }

    /**
     * The places of the body of a decision table: where the compiler found its conditions, what it returns and its
     * rules. The table is compiled with a rule in each column when its body is transposed.
     *
     * @param body the body of the table
     * @return the body and the look of each of its places
     */
    static BodyLayout.Placed rules(ThemedBody body) {
        return placed(body, Places.of(body));
    }

    /** The body with the look of each of its places, or with the base alone where the compiler laid out none. */
    private static BodyLayout.Placed placed(ThemedBody body, @Nullable Places places) {
        return places == null ? BodyLayout.Placed.plain(body)
                : new BodyLayout.Placed(body.rows(), Looks.of(body, places));
    }

    /**
     * A decision table as the compiler laid it out, and where its cells stand now.
     *
     * <p>The edits made since the table was compiled may have inserted or deleted rows and columns, or moved the
     * whole table on its sheet. A cell of the table as compiled stands where the edits moved it.
     *
     * @param node  the table, compiled
     * @param table where the table stands on its sheet now, header included
     * @param moves the rows and the columns the edits inserted or deleted since the table was compiled
     */
    record Compiled(TableSyntaxNode node, IGridRegion table, TableMoves moves) {

        /** The row of the sheet a cell of the compiled table stands in now, or {@link TableMoves#DELETED}. */
        int rowOf(ICell cell) {
            var row = moves.row(cell.getAbsoluteRow() - node.getGridTable().getRegion().getTop());
            return row == TableMoves.DELETED ? row : table.getTop() + row;
        }

        /** The column of the sheet a cell of the compiled table stands in now, or {@link TableMoves#DELETED}. */
        int columnOf(ICell cell) {
            var column = moves.column(cell.getAbsoluteColumn() - node.getGridTable().getRegion().getLeft());
            return column == TableMoves.DELETED ? column : table.getLeft() + column;
        }
    }

    /**
     * The axes of a table: a line holds a part of the table, such as a rule, and a cross runs along the lines, such
     * as a condition. A line is a row of a table written the usual way and a column of a table written the other way
     * round.
     *
     * @param transposed whether the table is written the other way round, with a rule in each column
     */
    private record Axes(boolean transposed) {

        int lineOf(ICell cell) {
            return lineOf(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        }

        int crossOf(ICell cell) {
            return crossOf(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        }

        int lineOf(int row, int column) {
            return transposed ? column : row;
        }

        int crossOf(int row, int column) {
            return transposed ? row : column;
        }

        int lineStart(IGridRegion region) {
            return transposed ? region.getLeft() : region.getTop();
        }

        int lineEnd(IGridRegion region) {
            return transposed ? region.getRight() : region.getBottom();
        }

        int crossStart(IGridRegion region) {
            return transposed ? region.getTop() : region.getLeft();
        }

        int crossEnd(IGridRegion region) {
            return transposed ? region.getBottom() : region.getRight();
        }

        ICell cellAt(IGrid grid, int line, int cross) {
            return transposed ? grid.getCell(line, cross) : grid.getCell(cross, line);
        }

        /**
         * A look as a table written the usual way takes it, turned to these axes: in a table written the other way
         * round, a line above a part is on its left. Turning a look twice gives it back.
         */
        ThemeStyle turned(ThemeStyle style) {
            return transposed && style.border() != null ? style.withBorder(style.border().transposed()) : style;
        }
    }

    /**
     * Where the parts of a decision table stand on the sheet, by line and by cross.
     *
     * @param axes        the axes of the table
     * @param code        the lines of the code of a Rules table
     * @param horizontals the lines of the values of the horizontal conditions
     * @param conditions  the crosses of the conditions, and of the column naming the rules
     * @param returns     the crosses of what the table returns or acts in
     * @param gridFrom    the first cross of the values a lookup returns, or {@link Integer#MAX_VALUE} for a table
     *                    that is not a lookup
     * @param rulesFrom   the first line of the rules
     * @param groups      the lines that set a group of rules apart: its first line, and the line after it
     */
    @Builder
    private record Places(Axes axes,
                          Set<Integer> code,
                          Set<Integer> horizontals,
                          Set<Integer> conditions,
                          Set<Integer> returns,
                          int gridFrom,
                          int rulesFrom,
                          Set<Integer> groups) {

        /**
         * The places of a compiled decision table, or {@code null} for a table that did not compile or the compiler
         * laid out no rows of.
         */
        static @Nullable Places of(ThemedBody body) {
            var compiled = body.compiled();
            if (!(compiled.node().getMember() instanceof DecisionTable decision)) {
                return null;
            }
            var conditions = rowsOf(decision.getConditionRows());
            var actions = rowsOf(decision.getActionRows());
            if (conditions.isEmpty() && actions.isEmpty()) {
                return null;
            }
            var reader = new Reader(decision, compiled, new Axes(body.transposed()));
            conditions.forEach(reader::readCondition);
            actions.forEach(reader::readAction);
            return reader.places(body.rows());
        }

        /**
         * The rows of a compiled table the compiler laid out: its conditions, or its actions, the returns among them.
         */
        private static List<FunctionalRow> rowsOf(Object @Nullable [] compiled) {
            var rows = new ArrayList<FunctionalRow>();
            if (compiled != null) {
                for (var element : compiled) {
                    if (element instanceof FunctionalRow row) {
                        rows.add(row);
                    }
                }
            }
            return rows;
        }
    }

    /** Collects the places of a table, one compiled row after another, where they stand now. */
    private static final class Reader {

        /** The axes of the table. */
        private final Axes axes;
        /** The table as it was compiled, and where its cells stand now. */
        private final Compiled compiled;
        /** Whether the table checks horizontal conditions. */
        private final boolean lookup;
        /** Whether the table declares its columns in rows of code, as a Rules table does. */
        private final boolean declared;
        /** How many rules the compiled table holds. */
        private final int rules;
        /** How many rules, one after another, share a value of the horizontal conditions. */
        private final int rulesPerHorizontal;
        private final Set<Integer> code = new HashSet<>();
        private final Set<Integer> titles = new HashSet<>();
        private final Set<Integer> horizontals = new HashSet<>();
        private final Set<Integer> conditions = new HashSet<>();
        private final Set<Integer> returns = new HashSet<>();
        private final Set<Integer> grid = new HashSet<>();

        /**
         * A reader of a compiled decision table.
         *
         * @param decision the table as the compiler laid it out
         * @param compiled the table as it was compiled, and where its cells stand now
         * @param axes     the axes of the table
         */
        Reader(DecisionTable decision, Compiled compiled, Axes axes) {
            var info = decision.getDtInfo();
            var node = compiled.node();
            this.axes = axes;
            this.compiled = compiled;
            this.lookup = info != null && info.getNumberHConditions() > 0;
            this.declared = !DecisionTableHelper.isSimple(node) && !DecisionTableHelper.isSmart(node);
            this.rules = decision.getNumberOfRules();
            this.rulesPerHorizontal = info == null ? 1 : info.getScale().getHScale().getMultiplier();
        }

        /** Reads where one condition stands: down the rules, or across the top of a lookup. */
        void readCondition(FunctionalRow row) {
            readCode(row);
            if (isHorizontal(row)) {
                // The values of a horizontal condition stand across the top, one for each column of the values a
                // lookup returns: every rule under a column is checked against the value over it.
                for (var rule = 0; rule < rules; rule += rulesPerHorizontal) {
                    var value = row.getValueCell(rule);
                    lines(value, horizontals);
                    crosses(value, grid);
                }
            } else {
                readColumn(row, conditions);
            }
        }

        /** Reads where one action stands, or what the table returns. */
        void readAction(FunctionalRow row) {
            readCode(row);
            if (lookup) {
                // What a lookup returns stands where its conditions meet; its titles are the horizontal conditions.
                for (var rule = 0; rule < rules; rule++) {
                    crosses(row.getValueCell(rule), grid);
                }
            } else {
                readColumn(row, returns);
            }
        }

        /** Reads the lines the code of a row takes, in a table that declares its columns in rows of code. */
        private void readCode(FunctionalRow row) {
            if (declared) {
                lines(row.getInfoTable(), code);
                lines(row.getCodeTable(), code);
                lines(row.getParamsTable(), code);
            }
        }

        /** Reads the lines the titles of a row take, and the crosses its titles and its values take. */
        private void readColumn(FunctionalRow row, Set<Integer> into) {
            lines(row.getPresentationTable(), titles);
            crosses(row.getPresentationTable(), into);
            crosses(firstValue(row), into);
        }

        private @Nullable ILogicalTable firstValue(FunctionalRow row) {
            return rules > 0 ? row.getValueCell(0) : null;
        }

        /** The places of the table, the column naming the rules and the groups found on the sheet as it stands. */
        Places places(ILogicalTable body) {
            var region = body.getSource().getRegion();
            var sheet = body.getSource().getGrid();
            var header = new HashSet<Integer>();
            header.addAll(code);
            header.addAll(titles);
            header.addAll(horizontals);
            var rulesFrom = header.stream().mapToInt(Integer::intValue).max().orElse(axes.lineStart(region) - 1) + 1;
            conditions.addAll(ruleNames(sheet, region));
            var gridFrom = grid.stream().mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE);
            returns.addAll(grid);
            return Places.builder()
                    .axes(axes)
                    .code(Set.copyOf(code))
                    .horizontals(Set.copyOf(horizontals))
                    .conditions(Set.copyOf(conditions))
                    .returns(Set.copyOf(returns))
                    .gridFrom(gridFrom)
                    .rulesFrom(rulesFrom)
                    .groups(Set.copyOf(groupsOf(sheet, region, rulesFrom)))
                    .build();
        }

        /**
         * The crosses of the column the table names its rules in, as the compiler finds it.
         *
         * <p>A Rules table names the kind of that column {@code RULE} in the first line of its code. A table matched
         * by its titles may name its rules in its first column only: there, a column the table reads no condition
         * and nothing it returns from.
         */
        private Set<Integer> ruleNames(IGrid sheet, IGridRegion region) {
            if (!declared) {
                var first = axes.crossStart(region);
                var taken = conditions.contains(first) || returns.contains(first) || grid.contains(first);
                return taken ? Set.of() : Set.of(first);
            }
            var kinds = code.stream().mapToInt(Integer::intValue).min().orElse(axes.lineStart(region));
            var found = new HashSet<Integer>();
            for (var cross = axes.crossStart(region); cross <= axes.crossEnd(region); cross++) {
                var kind = axes.cellAt(sheet, kinds, cross).getStringValue();
                if (DecisionTableColumnHeaders.RULE.getHeaderKey().equalsIgnoreCase(kind)) {
                    found.add(cross);
                }
            }
            return found;
        }

        /**
         * The lines that set a group of rules apart: the first line of the group, and the line after it. A group is
         * the rules a value of a condition is merged over. A rule written over several lines, with every value of it
         * merged over them, is one rule, not a group.
         */
        private Set<Integer> groupsOf(IGrid sheet, IGridRegion region, int rulesFrom) {
            var edges = new HashSet<Integer>();
            for (var cross : conditions) {
                for (var line = rulesFrom; line <= axes.lineEnd(region); line++) {
                    var merged = axes.cellAt(sheet, line, cross).getAbsoluteRegion();
                    if (axes.lineStart(merged) == line && axes.lineEnd(merged) > line
                            && splitAcross(sheet, region, merged)) {
                        edges.add(line);
                        edges.add(axes.lineEnd(merged) + 1);
                    }
                }
            }
            return edges;
        }

        /** Whether another column of the table is written in more than one cell along the lines of a merged value. */
        private boolean splitAcross(IGrid sheet, IGridRegion region, IGridRegion merged) {
            for (var cross = axes.crossStart(region); cross <= axes.crossEnd(region); cross++) {
                if (cross < axes.crossStart(merged) || cross > axes.crossEnd(merged)) {
                    var next = axes.cellAt(sheet, axes.lineStart(merged), cross).getAbsoluteRegion();
                    if (axes.lineEnd(next) < axes.lineEnd(merged)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** Adds the lines of the sheet a part of a row takes now. */
        private void lines(@Nullable ILogicalTable part, Set<Integer> into) {
            add(part, into, axes::lineOf);
        }

        /** Adds the crosses of the sheet a part of a row takes now. */
        private void crosses(@Nullable ILogicalTable part, Set<Integer> into) {
            add(part, into, axes::crossOf);
        }

        /**
         * Adds the line or the cross each cell of a part of a row stands in now. A cell an edit deleted adds nothing.
         *
         * @param part  the part of a compiled row
         * @param into  the lines or the crosses to add to
         * @param place the line or the cross of a row and a column of the sheet
         */
        private void add(@Nullable ILogicalTable part, Set<Integer> into, IntBinaryOperator place) {
            if (part == null) {
                return;
            }
            var source = part.getSource();
            for (var row = 0; row < source.getHeight(); row++) {
                for (var column = 0; column < source.getWidth(); column++) {
                    var cell = source.getCell(column, row);
                    var rowNow = compiled.rowOf(cell);
                    var columnNow = compiled.columnOf(cell);
                    if (rowNow != TableMoves.DELETED && columnNow != TableMoves.DELETED) {
                        into.add(place.applyAsInt(rowNow, columnNow));
                    }
                }
            }
        }

        /** Whether a condition is checked across the top of a lookup rather than down its rules. */
        private static boolean isHorizontal(FunctionalRow row) {
            return row.getName() != null
                    && row.getName().startsWith(DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
        }
    }

    /**
     * The looks of the places of one table, each laid over the base once rather than once per cell.
     *
     * <p>The looks are laid as a table written the usual way takes them and turned to the axes of the table for each
     * cell. The base is turned the other way before the looks are laid over it, so it keeps its own lines where it
     * names them.
     *
     * @param places      where the parts stand
     * @param base        the look every cell starts from, as a table written the usual way takes it
     * @param code        the look of the code, before its lines are fitted to the block it forms
     * @param titles      the look of a title of a condition
     * @param values      the look of a value of a condition
     * @param horizontals the look of a value of a horizontal condition
     * @param returnTitle the look of a title of what the table returns
     * @param returns     the look of a value the table returns
     * @param groups      the look laid over the first rule of a group and over the rule after it
     * @param codeFrom    the first line of the code
     * @param codeTo      the last line of the code
     */
    @Builder
    private record Looks(Places places,
                         ThemeStyle base,
                         ThemeStyle code,
                         ThemeStyle titles,
                         ThemeStyle values,
                         ThemeStyle horizontals,
                         ThemeStyle returnTitle,
                         ThemeStyle returns,
                         @Nullable ThemeStyle groups,
                         int codeFrom,
                         int codeTo) implements ThemeLayouts.PlaceLook {

        static Looks of(ThemedBody body, Places places) {
            var look = body.look();
            var upright = places.axes().turned(body.base());
            return Looks.builder()
                    .places(places)
                    .base(upright)
                    .code(upright.with(look.code()))
                    .titles(upright.with(look.titles()))
                    .values(upright.with(look.values()))
                    .horizontals(upright.with(look.horizontals()))
                    .returnTitle(upright.with(look.returnTitles()))
                    .returns(upright.with(look.returns()))
                    .groups(look.groups())
                    .codeFrom(places.code().stream().mapToInt(Integer::intValue).min().orElse(0))
                    .codeTo(places.code().stream().mapToInt(Integer::intValue).max().orElse(0))
                    .build();
        }

        /** The look of a cell, by the line and the cross of the sheet it stands in. */
        @Override
        public ThemeStyle at(ICell cell, int column, int row) {
            var axes = places.axes();
            return axes.turned(uprightAt(axes.lineOf(cell), axes.crossOf(cell), cell));
        }

        private ThemeStyle uprightAt(int line, int cross, ICell cell) {
            if (places.code().contains(line)) {
                return codeAt(cell);
            }
            var inGrid = cross >= places.gridFrom();
            if (places.horizontals().contains(line) && inGrid) {
                return horizontals;
            }
            if (line < places.rulesFrom()) {
                return headerAt(cross);
            }
            var style = valueAt(cross, inGrid);
            return places.groups().contains(line) ? style.with(groups) : style;
        }

        private ThemeStyle headerAt(int cross) {
            if (places.conditions().contains(cross)) {
                return titles;
            }
            return places.returns().contains(cross) ? returnTitle : base;
        }

        private ThemeStyle valueAt(int cross, boolean inGrid) {
            if (places.conditions().contains(cross)) {
                return values;
            }
            return inGrid || places.returns().contains(cross) ? returns : base;
        }

        /** The code with its lines above and below on the edges of the block it forms only, as the properties have. */
        private ThemeStyle codeAt(ICell cell) {
            var region = cell.getAbsoluteRegion();
            var axes = places.axes();
            return code.atEdges(base, axes.lineStart(region) <= codeFrom, axes.lineEnd(region) >= codeTo);
        }
    }
}
