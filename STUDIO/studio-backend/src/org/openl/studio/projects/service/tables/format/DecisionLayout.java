package org.openl.studio.projects.service.tables.format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DtColumnsDefinitionMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.util.StringUtils;

/**
 * Decides the role of each cell of the body of a decision table: a Rules, a SimpleRules, a SmartRules, a SimpleLookup
 * or a SmartLookup table. It also decides it for a Conditions, an Actions and a Returns table, which declare what
 * decision tables take by their titles, see {@link #conditions} and {@link #actions}.
 *
 * <p>The table is read as the compiler reads it. A Rules table declares its columns in rows of code above their
 * titles: the kind of each column, such as {@code C1} or {@code RET1}, its expression and its parameters. A
 * SimpleRules, a SmartRules and a lookup have none: the compiler matches their titles to the parameters instead. The
 * titles of a condition are titles of what the table takes, and its values are values, and so is the column a table
 * names its rules in. The titles of a column the table returns or acts in are titles of what it gives, and its values
 * are what it gives. A lookup also checks horizontal conditions, whose values stand across its top over the values it
 * returns: they title what the table gives, each row of them a region of its own, set apart from the row above, while
 * the titles of its vertical conditions in the corner above its rules title what it takes.
 *
 * <p>A value of a condition merged over several rules makes them a group, which the rule after the group closes. A
 * value of a horizontal condition merged over several columns of a lookup makes them a group as well. The first rule,
 * under the titles, starts no group, and nor does the first column of the values a lookup returns.
 *
 * <p>Where each part stands is taken from the compiled table: the lines of the sheet its code, its titles and its
 * horizontal conditions take, and the columns its conditions and what it returns take. Every line under them holds
 * rules.
 *
 * <p>A table the compiler read none of, because a type it names is missing, or laid out no rows of, is read from
 * its text, see {@link Guess}.
 *
 * <p>A table written the other way round, with a rule in each column, takes the roles with its rows and columns
 * swapped.
 */
final class DecisionLayout {

    /** The keyword naming the part of a declaration that holds its titles. */
    private static final String TITLE_KEYWORD = "title";

    /** The keywords naming the parts of a declaration of a Conditions, an Actions or a Returns table. */
    private static final Set<String> KEYWORDS = Set.of("inputs", "expression", "parameter", TITLE_KEYWORD);

    private DecisionLayout() {
    }

    /**
     * The places of the body of a decision table: where the compiler found its conditions, what it returns and its
     * rules. The table is compiled with a rule in each column when its body is transposed.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed rules(TableBody body) {
        var places = Places.of(body);
        return placed(body, places != null ? places : Guess.rules(body));
    }

    /**
     * The places of the body of a decision table the compiler read none of, as its text tells them, see {@link Guess}.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed guessedRules(TableBody body) {
        return placed(body, Guess.rules(body));
    }

    /**
     * The places of the body of a Conditions table: the declarations of conditions decision tables take by their
     * titles. The titles are titles of what a table takes.
     *
     * <p>Each declaration names its inputs, its expression, its parameters and its titles. The table holds each of
     * these parts in a row, a declaration in each column, as a Rules table holds its code and its titles above its
     * rules, and it takes their roles. The inputs, the expressions and the parameters are code. A keyword naming a
     * part, such as {@code Inputs} or {@code Title}, is a key.
     *
     * <p>The titles are found where the compiler reads them, by their keyword or by their place. A table written the
     * other way round, with a part in each column, takes the roles turned, as a table with a rule in each column does.
     * A table the compiler reads no declaration from is read from its text, see {@link Guess}.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed conditions(TableBody body) {
        return placed(body, Places.ofDeclarations(body, true));
    }

    /**
     * The places of the body of a Conditions table the compiler read no declaration from, as its text tells them.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed guessedConditions(TableBody body) {
        return placed(body, Guess.declarations(body, true));
    }

    /**
     * The places of the body of an Actions or a Returns table, laid out as a Conditions table is, see
     * {@link #conditions}. The titles are titles of what a table gives.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed actions(TableBody body) {
        return placed(body, Places.ofDeclarations(body, false));
    }

    /**
     * The places of the body of an Actions or a Returns table the compiler read no declaration from, as its text tells
     * them.
     *
     * @param body the body of the table
     * @return the body and the parts of each of its places
     */
    static BodyLayout.Placed guessedActions(TableBody body) {
        return placed(body, Guess.declarations(body, false));
    }

    /**
     * The body with what each of its places is, or holding values alone where the compiler laid out none, and the
     * lines that start a group: the rules after the first one a group sets apart, and the columns of a lookup after
     * the first one its horizontal groups set apart.
     */
    private static BodyLayout.Placed placed(TableBody body, @Nullable Places places) {
        if (places == null) {
            return BodyLayout.Placed.plain(body);
        }
        var lines = places.groups()
                .stream()
                .filter(line -> line > places.rulesFrom())
                .collect(Collectors.toUnmodifiableSet());
        var crosses = places.columns()
                .stream()
                .filter(cross -> cross > places.gridFrom())
                .collect(Collectors.toUnmodifiableSet());
        var transposed = places.axes().transposed();
        return new BodyLayout.Placed(body.rows(), new Roles(places), transposed ? crosses : lines,
                transposed ? lines : crosses);
    }

    /**
     * The axes of a table: a line holds a part of the table, such as a rule, and a cross runs along the lines, such
     * as a condition. A line is a row of a table written the usual way and a column of a table written the other way
     * round.
     *
     * @param transposed whether the table is written the other way round: with a rule in each column, or with each
     *                   part of its declarations in a column
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

        /** The axes the other way round: the lines of these are the crosses of those. */
        Axes swapped() {
            return new Axes(!transposed);
        }
    }

    /**
     * Where the parts of a decision table, or of a table declaring what decision tables take, stand on the sheet, by
     * line and by cross.
     *
     * @param axes        the axes of the table
     * @param code        the lines of the code of a Rules table, or of the inputs, the expressions and the parameters
     *                    of the declarations
     * @param horizontals the lines of the values of the horizontal conditions
     * @param conditions  the crosses of the conditions, and of the column naming the rules
     * @param ruleNames   the crosses of the column naming the rules
     * @param keywords    the lines whose first cross names the part of a declaration they hold, such as
     *                    {@code Inputs} or {@code Title}
     * @param returns     the crosses of what the table returns or acts in
     * @param gridFrom    the first cross of the values a lookup returns, or {@link Integer#MAX_VALUE} for a table
     *                    that is not a lookup
     * @param rulesFrom   the first line of the rules
     * @param groups      the lines that set a group of rules apart: its first line, and the line after it
     * @param columns     the crosses that set a group of columns of a lookup apart: its first cross, and the cross
     *                    after it
     */
    @Builder
    private record Places(Axes axes,
                          Set<Integer> code,
                          Set<Integer> horizontals,
                          Set<Integer> conditions,
                          Set<Integer> ruleNames,
                          Set<Integer> keywords,
                          Set<Integer> returns,
                          int gridFrom,
                          int rulesFrom,
                          Set<Integer> groups,
                          Set<Integer> columns) {

        /**
         * The places of a compiled decision table, or {@code null} for a table that did not compile or the compiler
         * laid out no rows of.
         */
        static @Nullable Places of(TableBody body) {
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
         * The places of a table that declares what decision tables take, or {@code null} for a table the compiler
         * read no declaration from: its titles where the compiler found them, and every other line of its body as
         * code. Every declaration stands in a cross, a condition or what is returned, and the body holds no rules.
         *
         * @param conditions whether the table declares conditions, rather than actions or returns
         */
        static @Nullable Places ofDeclarations(TableBody body, boolean conditions) {
            var compiled = body.compiled();
            var titles = compiled.node().getMetaInfoReader() instanceof DtColumnsDefinitionMetaInfoReader reader
                    ? reader.getBoundNode().getTitles()
                    : null;
            if (titles == null) {
                return null;
            }
            // The compiler reads a part in each column, so the table it reads transposed holds a part in each row, as
            // the code and the titles of a Rules table stand.
            var axes = new Axes(titles.isNormalOrientation());
            var titleLines = new HashSet<Integer>();
            compiled.addPlaces(titles, axes::lineOf, titleLines);
            return declared(body, axes, titleLines, conditions);
        }

        /**
         * The places of a table that declares what decision tables take: its titles in the given lines, and every
         * other line of its body as code.
         */
        private static Places declared(TableBody body, Axes axes, Set<Integer> titleLines, boolean conditions) {
            var region = body.rows().getSource().getRegion();
            var code = IntStream.rangeClosed(axes.lineStart(region), axes.lineEnd(region))
                    .filter(line -> !titleLines.contains(line))
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
            var declarations = IntStream.rangeClosed(axes.crossStart(region), axes.crossEnd(region))
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
            return Places.builder()
                    .axes(axes)
                    .code(code)
                    .horizontals(Set.of())
                    .conditions(conditions ? declarations : Set.of())
                    .ruleNames(Set.of())
                    .keywords(keywordLines(body, axes, region))
                    .returns(conditions ? Set.of() : declarations)
                    .gridFrom(Integer.MAX_VALUE)
                    .rulesFrom(axes.lineEnd(region) + 1)
                    .groups(Set.of())
                    .columns(Set.of())
                    .build();
        }

        /**
         * The lines of a table declaring what decision tables take whose first cross names the part of a declaration
         * they hold, as the compiler finds the parts by their keyword: {@code Inputs}, {@code Expression},
         * {@code Parameter} and {@code Title}, in any case.
         */
        private static Set<Integer> keywordLines(TableBody body, Axes axes, IGridRegion region) {
            return keywordLines(body, axes, region, KEYWORDS::contains);
        }

        /**
         * The lines of a table declaring what decision tables take whose first cross names a part the test accepts,
         * its text trimmed and in lower case.
         */
        private static Set<Integer> keywordLines(TableBody body, Axes axes, IGridRegion region,
                                                 Predicate<String> named) {
            var sheet = body.rows().getSource().getGrid();
            var first = axes.crossStart(region);
            return IntStream.rangeClosed(axes.lineStart(region), axes.lineEnd(region))
                    .filter(line -> named.test(StringUtils.trimToEmpty(
                            axes.cellAt(sheet, line, first).getStringValue()).toLowerCase(Locale.ROOT)))
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
        }

        /**
         * The places of a decision table that holds rules, its groups found on the sheet as it stands: the lines a
         * value of a condition is merged over, and the crosses of a lookup a horizontal value is merged over.
         */
        private static Places ofRules(ILogicalTable rows, Axes axes, Set<Integer> code, Set<Integer> horizontals,
                              Set<Integer> conditions, Set<Integer> ruleNames, Set<Integer> returns, int gridFrom,
                              int rulesFrom) {
            var region = rows.getSource().getRegion();
            var sheet = rows.getSource().getGrid();
            return Places.builder()
                    .axes(axes)
                    .code(Set.copyOf(code))
                    .horizontals(Set.copyOf(horizontals))
                    .conditions(Set.copyOf(conditions))
                    .ruleNames(Set.copyOf(ruleNames))
                    .keywords(Set.of())
                    .returns(Set.copyOf(returns))
                    .gridFrom(gridFrom)
                    .rulesFrom(rulesFrom)
                    .groups(Reader.groupEdges(axes, conditions, rulesFrom, sheet, region))
                    // The columns of a lookup are grouped by its horizontal conditions as its rules are by the others.
                    .columns(Reader.groupEdges(axes.swapped(), horizontals, gridFrom, sheet, region))
                    .build();
        }

        /**
         * The rows of a compiled table the compiler laid out: its conditions, or its actions, the returns among them.
         */
        private static List<FunctionalRow> rowsOf(Object @Nullable [] compiled) {
            return Optional.ofNullable(compiled).stream()
                    .flatMap(Arrays::stream)
                    .filter(FunctionalRow.class::isInstance)
                    .map(FunctionalRow.class::cast)
                    .toList();
        }
    }

    /** Collects the places of a table, one compiled row after another. */
    private static final class Reader {

        /** The axes of the table. */
        private final Axes axes;
        /** The table as it was compiled. */
        private final CompiledTable compiled;
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
         * @param compiled the table as it was compiled
         * @param axes     the axes of the table
         */
        Reader(DecisionTable decision, CompiledTable compiled, Axes axes) {
            var info = decision.getDtInfo();
            var node = compiled.node();
            this.axes = axes;
            this.compiled = compiled;
            this.lookup = info != null && info.getNumberHConditions() > 0;
            this.declared = !DecisionTableHelper.isSimple(node) && !DecisionTableHelper.isSmart(node);
            this.rules = decision.getNumberOfRules();
            this.rulesPerHorizontal = Optional.ofNullable(info)
                    .map(found -> found.getScale().getHScale().getMultiplier())
                    .orElse(1);
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
            var ruleNames = ruleNames(sheet, region);
            conditions.addAll(ruleNames);
            var gridFrom = grid.stream().mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE);
            returns.addAll(grid);
            return Places.ofRules(body, axes, code, horizontals, conditions, ruleNames, returns, gridFrom, rulesFrom);
        }

        /**
         * The crosses of the column the table names its rules in, as the compiler finds it.
         *
         * <p>A Rules table names the kind of that column {@code RULE} in the first line of its code. A table matched
         * by its titles names its rules in the column whose title the compiler matched as the one of the rules, and a
         * title it matched as nothing names no rules.
         */
        private Set<Integer> ruleNames(IGrid sheet, IGridRegion region) {
            if (!declared) {
                return matchedRuleNames();
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

        /** The crosses of the titles the compiler matched as the one of the rules. */
        private Set<Integer> matchedRuleNames() {
            if (!(compiled.node().getMetaInfoReader() instanceof DecisionTableMetaInfoReader reader)) {
                return Set.of();
            }
            var found = new HashSet<Integer>();
            reader.getMetaInfos()
                    .getRules()
                    .forEach(title -> found.add(axes.crossOf(title.getRow(), title.getColumn())));
            return found;
        }

        /**
         * The lines that set a group apart: the first line of the group, and the line after it. A group is the lines
         * a value of one of the crosses is merged over, such as the rules a value of a condition is merged over. A
         * line written over several lines of the sheet, with every value of it merged over them, is one line, not a
         * group.
         *
         * @param axes    the axes the lines and the crosses run along
         * @param crosses the crosses whose merged values make groups
         * @param from    the first line a group can start at
         * @param sheet   the sheet of the table
         * @param region  where the body of the table stands on the sheet
         */
        private static Set<Integer> groupEdges(Axes axes, Set<Integer> crosses, int from, IGrid sheet,
                                               IGridRegion region) {
            var edges = new HashSet<Integer>();
            for (var cross : crosses) {
                for (var line = from; line <= axes.lineEnd(region); line++) {
                    var merged = axes.cellAt(sheet, line, cross).getAbsoluteRegion();
                    if (axes.lineStart(merged) == line && axes.lineEnd(merged) > line
                            && splitAcross(axes, sheet, region, merged)) {
                        edges.add(line);
                        edges.add(axes.lineEnd(merged) + 1);
                    }
                }
            }
            return Set.copyOf(edges);
        }

        /** Whether another cross of the table is written in more than one cell along the lines of a merged value. */
        private static boolean splitAcross(Axes axes, IGrid sheet, IGridRegion region, IGridRegion merged) {
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

        /** Adds the lines of the sheet a part of a row takes. */
        private void lines(@Nullable ILogicalTable part, Set<Integer> into) {
            add(part, into, axes::lineOf);
        }

        /** Adds the crosses of the sheet a part of a row takes. */
        private void crosses(@Nullable ILogicalTable part, Set<Integer> into) {
            add(part, into, axes::crossOf);
        }

        /**
         * Adds the line or the cross each cell of a part of a row stands in.
         *
         * @param part  the part of a compiled row
         * @param into  the lines or the crosses to add to
         * @param place the line or the cross of a row and a column of the sheet
         */
        private void add(@Nullable ILogicalTable part, Set<Integer> into, IntBinaryOperator place) {
            Optional.ofNullable(part)
                    .map(ILogicalTable::getSource)
                    .ifPresent(source -> compiled.addPlaces(source, place, into));
        }

        /** Whether a condition is checked across the top of a lookup rather than down its rules. */
        private static boolean isHorizontal(FunctionalRow row) {
            return row.getName() != null
                    && row.getName().startsWith(DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
        }
    }

    /**
     * Where the parts of a decision table the compiler read none of stand, as its text tells them.
     *
     * <ul>
     *     <li>A Rules table names the kind of each column in the first line of its code, such as {@code C1},
     *     {@code RET1} or {@code RULE}: the code takes three lines, the expressions and the parameters under the
     *     kinds, and the titles the fourth. A condition of any kind is a condition, an action or a return is what the
     *     table returns or acts in, and {@code RULE} names the rules.</li>
     *     <li>A SimpleRules and a SmartRules table title its columns in its first line: the first as many columns as
     *     the header names parameters are its conditions, keeping at least the last one for what it returns.</li>
     *     <li>A lookup writes its horizontal conditions across its top, one line each, as tall as the title of its
     *     first column. The parameters the horizontal conditions leave are its vertical conditions, at least one, and
     *     the columns after them hold what it returns.</li>
     *     <li>A Conditions, an Actions and a Returns table name the parts of their declarations by their keywords:
     *     the part titled {@code Title} holds the titles. With no keywords, the parts stand in columns, the titles in
     *     the fourth, or the last.</li>
     * </ul>
     */
    private static final class Guess {

        /** The lines of code of a Rules table: the kinds, the expressions and the parameters. */
        private static final int CODE_LINES = 3;

        /** The part of a declaration that holds the titles, when the table names no part by its keyword. */
        private static final int TITLE_PART = 3;

        private Guess() {
        }

        /** The places of a decision table, or {@code null} for a Rules table naming no kind of column. */
        static @Nullable Places rules(TableBody body) {
            var node = body.compiled().node();
            if (DecisionTableHelper.isSimpleLookupTable(node) || DecisionTableHelper.isSmartLookupTable(node)) {
                return lookup(body);
            }
            if (DecisionTableHelper.isSimple(node) || DecisionTableHelper.isSmart(node)) {
                return matched(body);
            }
            return declared(body);
        }

        /** A Rules table: its code, its titles, and the crosses the kinds of its columns name. */
        private static @Nullable Places declared(TableBody body) {
            var axes = new Axes(body.transposed());
            var upright = body.upright();
            var conditions = new HashSet<Integer>();
            var ruleNames = new HashSet<Integer>();
            var returns = new HashSet<Integer>();
            for (var column = 0; column < upright.getWidth(); column++) {
                var cell = upright.getCell(column, 0);
                var crosses = crosses(axes, cell.getAbsoluteRegion());
                var kind = GuessedReads.columnKindOf(cell.getStringValue());
                switch (kind) {
                    case RULE -> {
                        ruleNames.addAll(crosses);
                        conditions.addAll(crosses);
                    }
                    case ACTION, RETURN, COLLECT_RETURN -> returns.addAll(crosses);
                    case null -> {
                        // A column of no kind the compiler knows holds values.
                    }
                    default -> conditions.addAll(crosses);
                }
            }
            if (conditions.isEmpty() && returns.isEmpty()) {
                return null;
            }
            // The lines of the body, as the compiler reads them: a line is as tall as the cell of its first column.
            var code = new HashSet<Integer>();
            for (var line = 0; line < Math.min(CODE_LINES, upright.getHeight()); line++) {
                code.addAll(lines(axes, upright.getCell(0, line).getAbsoluteRegion()));
            }
            var region = body.rows().getSource().getRegion();
            var titlesEnd = upright.getHeight() > CODE_LINES
                    ? axes.lineEnd(upright.getCell(0, CODE_LINES).getAbsoluteRegion())
                    : axes.lineEnd(region);
            return Places.ofRules(body.rows(), axes, code, Set.of(), conditions, ruleNames, returns,
                    Integer.MAX_VALUE, titlesEnd + 1);
        }

        /** A SimpleRules or a SmartRules table: its titles, its conditions by the parameters, then what it returns. */
        private static Places matched(TableBody body) {
            var axes = new Axes(false);
            var rows = body.rows();
            var count = Math.min(parametersOf(body), rows.getWidth() - 1);
            var conditions = new HashSet<Integer>();
            var returns = new HashSet<Integer>();
            var titlesEnd = Integer.MIN_VALUE;
            for (var column = 0; column < rows.getWidth(); column++) {
                var region = rows.getCell(column, 0).getAbsoluteRegion();
                (column < count ? conditions : returns).addAll(crosses(axes, region));
                titlesEnd = Math.max(titlesEnd, axes.lineEnd(region));
            }
            return Places.ofRules(body.rows(), axes, Set.of(), Set.of(), conditions, Set.of(), returns,
                    Integer.MAX_VALUE, titlesEnd + 1);
        }

        /** A lookup: its horizontal conditions across the top, its vertical conditions, and the grid it returns. */
        private static Places lookup(TableBody body) {
            var axes = new Axes(false);
            var rows = body.rows();
            var region = rows.getSource().getRegion();
            var horizontals = lines(axes, rows.getCell(0, 0).getAbsoluteRegion());
            var count = Math.clamp((long) parametersOf(body) - horizontals.size(), 1, Math.max(1, rows.getWidth() - 1));
            var conditions = new HashSet<Integer>();
            for (var column = 0; column < Math.min(count, rows.getWidth()); column++) {
                conditions.addAll(crosses(axes, rows.getCell(column, 0).getAbsoluteRegion()));
            }
            var gridFrom = conditions.stream().mapToInt(Integer::intValue).max().orElse(axes.crossStart(region)) + 1;
            var grid = IntStream.rangeClosed(gridFrom, axes.crossEnd(region))
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
            var rulesFrom = horizontals.stream().mapToInt(Integer::intValue).max().orElse(axes.lineStart(region)) + 1;
            return Places.ofRules(body.rows(), axes, Set.of(), horizontals, conditions, Set.of(), grid, gridFrom,
                    rulesFrom);
        }


        /**
         * A Conditions, an Actions or a Returns table: the parts of its declarations named by their keywords, or by
         * their place, and the titles among them.
         */
        static Places declarations(TableBody body, boolean conditions) {
            var rows = body.rows();
            var region = rows.getSource().getRegion();
            // The parts stand in rows when the first column names more of them than the first row, else in columns.
            var axes = new Axes(keywordsIn(rows.transpose()) <= keywordsIn(rows));
            var titles = Places.keywordLines(body, axes, region, TITLE_KEYWORD::equals);
            if (titles.isEmpty()) {
                var parts = axes.transposed() ? rows : rows.transpose();
                var part = Math.min(TITLE_PART, parts.getWidth() - 1);
                titles = lines(axes, parts.getCell(part, 0).getAbsoluteRegion());
            }
            return Places.declared(body, axes, titles, conditions);
        }

        /** How many parts of a declaration the first row of a body names by their keywords. */
        private static long keywordsIn(ILogicalTable rows) {
            return IntStream.range(0, rows.getWidth())
                    .mapToObj(column -> GuessedReads.textAt(rows, column, 0).toLowerCase(Locale.ROOT))
                    .filter(KEYWORDS::contains)
                    .count();
        }

        private static int parametersOf(TableBody body) {
            return GuessedReads.parametersOf(GuessedReads.headerOf(body.compiled().node()));
        }

        private static Set<Integer> lines(Axes axes, IGridRegion region) {
            return IntStream.rangeClosed(axes.lineStart(region), axes.lineEnd(region))
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
        }

        private static Set<Integer> crosses(Axes axes, IGridRegion region) {
            return IntStream.rangeClosed(axes.crossStart(region), axes.crossEnd(region))
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * What the places of one table are.
     *
     * <p>The code is code the compiler reads, and a keyword naming a part of a declaration is its key. The titles of
     * the conditions and of the column naming the rules are titles of what the table takes; the titles of what it
     * returns or acts in, and the values of the horizontal conditions of a lookup over the grid it returns, titles of
     * what it gives. Under them, the names of the rules are keys, the values of the conditions values the table
     * takes, and what it returns, the grid of a lookup included, what it gives.
     *
     * @param places where the parts stand
     */
    private record Roles(Places places) implements TableRegionLayout.PlaceRoles {

        private static final Place CODE = Place.of(RawTableRole.CODE);
        private static final Place KEY = Place.of(RawTableRole.KEY);
        private static final Place TITLE = Place.of(RawTableRole.INPUT_TITLE);
        private static final Place RESULT_TITLE = Place.of(RawTableRole.OUTPUT_TITLE);
        /** A horizontal value of a lookup: each row of them is set apart from the row above. */
        private static final Place HORIZONTAL = RESULT_TITLE.byRows();

        /** What a cell is, by the line and the cross of the sheet it stands in. */
        @Override
        public Place at(ICell cell, int column, int row) {
            var axes = places.axes();
            var line = axes.lineOf(cell);
            var cross = axes.crossOf(cell);
            var keyword = places.keywords().contains(line) && isFirstCross(cross);
            if (places.code().contains(line)) {
                return keyword ? KEY : CODE;
            }
            var inGrid = cross >= places.gridFrom();
            if (places.horizontals().contains(line) && inGrid) {
                return HORIZONTAL;
            }
            if (line < places.rulesFrom()) {
                return keyword ? KEY : titleAt(cross);
            }
            return Place.of(roleOfValue(cross, inGrid));
        }

        /** Whether a cross is the first one of the declarations, the one naming their parts. */
        private boolean isFirstCross(int cross) {
            return places.conditions().stream().allMatch(other -> other >= cross)
                    && places.returns().stream().allMatch(other -> other >= cross);
        }

        /** A title: of what the table takes, or of what it gives. */
        private Place titleAt(int cross) {
            return places.returns().contains(cross) && !places.conditions().contains(cross) ? RESULT_TITLE : TITLE;
        }

        /** The role of a value: the name of a rule, a value of a condition, or what the table gives. */
        private RawTableRole roleOfValue(int cross, boolean inGrid) {
            if (places.ruleNames().contains(cross)) {
                return RawTableRole.KEY;
            }
            if (places.conditions().contains(cross)) {
                return RawTableRole.VALUE;
            }
            return inGrid || places.returns().contains(cross) ? RawTableRole.OUTPUT : RawTableRole.VALUE;
        }
    }
}
