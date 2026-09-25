package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableBoundNode;
import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ArrayHolder;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;

@Slf4j
public class DecisionTableMetaInfoReader extends AMethodMetaInfoReader<DecisionTableBoundNode> {
    private final DecisionTable decisionTable;
    private Map<CellKey, CellMetaInfo> preparedMetaInfos;
    private final Deque<MetaInfoHolder> stack;

    public DecisionTableMetaInfoReader(DecisionTableBoundNode boundNode) {
        this(boundNode, null);
    }

    public DecisionTableMetaInfoReader(DecisionTableBoundNode boundNode, DecisionTable decisionTable) {
        super(boundNode);
        this.decisionTable = decisionTable;
        this.stack = new ArrayDeque<>();
        this.stack.push(new MetaInfoHolder());
    }

    public static class MetaInfoHolder {
        /**
         * Map for condition cells in header to parameter index
         */
        @Getter
        private final Map<CellKey, List<HeaderMetaInfo>> conditions = new HashMap<>();

        /**
         * Map for action cells in header to parameter index
         */
        @Getter
        private final Map<CellKey, HeaderMetaInfo> actions = new HashMap<>();

        /**
         * Map for compound return column descriptions in SimpleRules header
         */
        @Getter
        private final Map<CellKey, ReturnMetaInfo> returns = new HashMap<>();

        /**
         * List for inputParameter mapping details for smart dt
         */
        @Getter
        private final List<Pair<String, String>> parametersToReturn = new ArrayList<>();

        @Getter
        private final List<CellKey> unmatched = new ArrayList<>();

        @Getter
        private final List<CellKey> rules = new ArrayList<>();

        public void merge(MetaInfoHolder metaInfoHolder) {
            if (metaInfoHolder == null) {
                return;
            }
            conditions.putAll(metaInfoHolder.conditions);
            actions.putAll(metaInfoHolder.actions);
            returns.putAll(metaInfoHolder.returns);
            parametersToReturn.addAll(metaInfoHolder.parametersToReturn);
            unmatched.addAll(metaInfoHolder.unmatched);
            rules.addAll(metaInfoHolder.rules);
        }
    }

    public void pushMetaInfos() {
        stack.push(new MetaInfoHolder());
    }

    public MetaInfoHolder popMetaInfos() {
        return stack.pop();
    }

    public MetaInfoHolder getMetaInfos() {
        return stack.getFirst();
    }

    @Override
    protected String getAdditionalMetaInfoForTableReturnType() {
        final List<Pair<String, String>> inputParametersToReturn = getMetaInfos().getParametersToReturn();
        if (inputParametersToReturn.isEmpty()) {
            return null;
        } else {
            var sb = new StringBuilder();
            inputParametersToReturn.sort(Map.Entry.comparingByKey());
            for (Pair<String, String> p : inputParametersToReturn) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append("Input ");
                sb.append(p.getKey());
                sb.append(" is set to return ");
                sb.append(p.getValue());
            }
            return sb.toString();
        }
    }

    @Override
    public void prepare(IGridRegion region) {
        try {
            if (preparedMetaInfos == null) {
                preparedMetaInfos = new HashMap<>();
            }
            var decisionTable = getDecisionTable();

            saveSimpleRulesMetaInfo(region);
            saveCompoundReturnColumn(region);

            var conditionRows = decisionTable.getConditionRows();
            var actionRows = decisionTable.getActionRows();

            if (!DecisionTableHelper.isSmart(decisionTable.getSyntaxNode()) && !DecisionTableHelper
                    .isSimple(decisionTable.getSyntaxNode())) {
                if (conditionRows != null) {
                    // Condition description
                    for (IBaseCondition conditionRow : conditionRows) {
                        saveExpressionMetaInfo((FunctionalRow) conditionRow, region);
                    }
                }

                if (actionRows != null) {
                    // Action description
                    for (IBaseAction action : actionRows) {
                        saveExpressionMetaInfo((FunctionalRow) action, region);
                    }
                }
            }
            if (conditionRows != null) {
                // Condition values
                for (IBaseCondition condition : conditionRows) {
                    var funcRow = (FunctionalRow) condition;
                    saveValueMetaInfo(funcRow, region);
                }
            }
            if (actionRows != null) {
                // Action values
                for (IBaseAction action : actionRows) {
                    var funcRow = (FunctionalRow) action;
                    saveValueMetaInfo(funcRow, region);
                }
            }
        } catch (Exception e) {
            // Something unexpected is occurred. Work without full meta info.
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void release() {
        preparedMetaInfos = null;
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        return getPreparedMetaInfo(row, col);
    }

    private void setMetaInfo(CellKey key,
                             List<HeaderMetaInfo> headerMetaInfos,
                             IGridRegion region,
                             Function<HeaderMetaInfo, String> headerToString) {
        if (headerMetaInfos.size() > 2) {
            return;
        }
        var row = key.getRow();
        var col = key.getColumn();
        if (!IGridRegion.Tool.contains(region, col, row)) {
            return;
        }

        // SimpleRules or SimpleLookup
        var grid = getGridTable().getGrid();
        var cellValue = grid.getCell(col, row).getStringValue();
        if (StringUtils.isBlank(cellValue)) {
            return;
        }
        var start = 0;
        int end = headerMetaInfos.size() > 1
                ? cellValue
                .indexOf(DecisionTableHelper.HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER)
                : cellValue.length();
        var simpleNodeUsages = new ArrayList<SimpleNodeUsage>();
        for (HeaderMetaInfo headerMetaInfo : headerMetaInfos) {
            var text = headerToString.apply(headerMetaInfo);
            var simpleNodeUsage = new SimpleNodeUsage(start,
                    end,
                    text,
                    headerMetaInfo.getUrl(),
                    headerMetaInfo.getUrl() != null ? NodeType.OTHERUNDERLINED : NodeType.OTHER);
            simpleNodeUsages.add(simpleNodeUsage);
            start = end + 2;
            end = cellValue.length();
        }
        setPreparedMetaInfo(row, col, new CellMetaInfo(JavaOpenClass.STRING, false, simpleNodeUsages));
    }

    private String buildConditionHint(HeaderMetaInfo headerMetaInfo) {
        var parameterNames = headerMetaInfo.getParameterNames();
        var header = headerMetaInfo.getHeader();
        var statement = headerMetaInfo.getConditionStatement();
        var columnTypes = headerMetaInfo.getColumnTypes();

        var sb = new StringBuilder();
        sb.append("Condition: ").append(header);
        if (!StringUtils.isEmpty(statement)) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("Expression: ").append(statement.replace("\n", StringUtils.SPACE));
        }
        if (!StringUtils.isEmpty(headerMetaInfo.getAdditionalDetails())) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(headerMetaInfo.getAdditionalDetails());
        }
        appendParameters(sb, parameterNames, columnTypes);
        return sb.toString();
    }

    public static void appendParameters(StringBuilder sb, String[] parameterNames, IOpenClass[] columnTypes) {
        if (columnTypes == null || columnTypes.length == 0) {
            return;
        }
        var i = 0;
        if (!sb.isEmpty()) {
            sb.append("\n");
        }
        if (columnTypes.length > 1) {
            if (parameterNames != null && parameterNames.length > 0 && Arrays.stream(parameterNames)
                    .allMatch(Objects::nonNull)) {
                sb.append("Parameters: ");
            } else {
                sb.append("Types: ");
            }
        } else {
            if (parameterNames != null && parameterNames.length > 0) {
                sb.append("Parameter: ");
            } else {
                sb.append("Type: ");
            }
        }
        for (IOpenClass type : columnTypes) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(type.getDisplayName(INamedThing.SHORT));
            if (parameterNames != null && i < parameterNames.length && parameterNames[i] != null) {
                sb.append(StringUtils.SPACE).append(parameterNames[i]);
            }
            i++;
        }
    }

    private String buildActionHint(HeaderMetaInfo headerMetaInfo) {
        var parameterNames = headerMetaInfo.getParameterNames();
        var header = headerMetaInfo.getHeader();
        var statement = headerMetaInfo.getConditionStatement();
        var columnTypes = headerMetaInfo.getColumnTypes();
        var sb = new StringBuilder();
        sb.append("Action: ").append(header);
        if (!StringUtils.isEmpty(statement)) {
            sb.append("\n").append("Expression: ").append(statement.replace("\n", StringUtils.SPACE));
        }
        if (!StringUtils.isEmpty(headerMetaInfo.getAdditionalDetails())) {
            sb.append("\n").append(headerMetaInfo.getAdditionalDetails());
        }
        appendParameters(sb, parameterNames, columnTypes);
        return sb.toString();
    }

    private void saveSimpleRulesMetaInfo(IGridRegion region) {
        final Map<CellKey, List<HeaderMetaInfo>> simpleRulesConditionMap = getMetaInfos().getConditions();
        for (Map.Entry<CellKey, List<HeaderMetaInfo>> entry : simpleRulesConditionMap.entrySet()) {
            setMetaInfo(entry.getKey(), entry.getValue(), region, this::buildConditionHint);
        }
        final Map<CellKey, HeaderMetaInfo> simpleRulesActionMap = getMetaInfos().getActions();
        for (Map.Entry<CellKey, HeaderMetaInfo> entry : simpleRulesActionMap.entrySet()) {
            setMetaInfo(entry.getKey(), List.of(entry.getValue()), region, this::buildActionHint);
        }
        final List<CellKey> unmatched = getMetaInfos().getUnmatched();
        for (CellKey cellKey : unmatched) {
            setMetaInfo(cellKey, "Unmatched column");
        }
        final List<CellKey> rules = getMetaInfos().getRules();
        for (CellKey cellKey : rules) {
            setMetaInfo(cellKey, "Rule column");
        }
    }

    private void setMetaInfo(CellKey cellKey, String description) {
        var grid = getGridTable().getGrid();
        var cellValue = grid.getCell(cellKey.getColumn(), cellKey.getRow()).getStringValue();
        if (StringUtils.isNotEmpty(cellValue)) {
            var nodeUsage = new SimpleNodeUsage(0, cellValue.length(), description, null, NodeType.OTHER);
            setPreparedMetaInfo(cellKey.getRow(),
                    cellKey.getColumn(),
                    new CellMetaInfo(JavaOpenClass.STRING, false, List.of(nodeUsage)));
        }
    }

    private void saveCompoundReturnColumn(IGridRegion region) {
        final Map<CellKey, ReturnMetaInfo> simpleRulesReturnDescriptions = getMetaInfos().getReturns();
        for (Map.Entry<CellKey, ReturnMetaInfo> entry : simpleRulesReturnDescriptions.entrySet()) {
            var key = entry.getKey();
            var row = key.getRow();
            var col = key.getColumn();
            if (!IGridRegion.Tool.contains(region, col, row)) {
                continue;
            }

            var cell = getGridTable().getGrid().getCell(col, row);
            var stringValue = cell.getStringValue();

            if (StringUtils.isBlank(stringValue)) {
                continue;
            }
            var returnMetaInfo = entry.getValue();
            var simpleNodeUsage = new SimpleNodeUsage(0,
                    stringValue.length(),
                    returnMetaInfo.getDetails(),
                    returnMetaInfo.getUri(),
                    returnMetaInfo.getUri() != null ? NodeType.OTHERUNDERLINED : NodeType.OTHER);
            var metaInfo = new CellMetaInfo(JavaOpenClass.STRING,
                    false,
                    List.of(simpleNodeUsage));
            setPreparedMetaInfo(row, col, metaInfo);
        }
    }

    public void addCondition(int row,
                             int col,
                             String header,
                             String[] parameterNames,
                             String statement,
                             IOpenClass[] columnTypes,
                             String url,
                             String additionalDetails,
                             boolean vertical) {
        var headerMetaInfos = getMetaInfos().getConditions()
                .computeIfAbsent(CellKey.CellKeyFactory.getCellKey(col, row), e -> new ArrayList<>());
        headerMetaInfos
                .add(new HeaderMetaInfo(header, parameterNames, statement, columnTypes, url, additionalDetails, vertical));
    }

    public void addUnmatched(int row, int col) {
        getMetaInfos().getUnmatched().add(CellKey.CellKeyFactory.getCellKey(col, row));
    }

    public void addRule(int row, int col) {
        getMetaInfos().getRules().add(CellKey.CellKeyFactory.getCellKey(col, row));
    }

    public void addAction(int row,
                          int col,
                          String header,
                          String[] parameterNames,
                          String statement,
                          IOpenClass[] columnTypes,
                          String url,
                          String additionalInfo) {
        getMetaInfos().getActions()
                .put(CellKey.CellKeyFactory.getCellKey(col, row),
                        new HeaderMetaInfo(header, parameterNames, statement, columnTypes, url, additionalInfo, false));
    }

    public void addReturn(int row, int col, String details, String uri) {
        getMetaInfos().getReturns().put(CellKey.CellKeyFactory.getCellKey(col, row), new ReturnMetaInfo(details, uri));
    }

    public void addParameterToReturn(String parameterStatement, String returnStatement) {
        getMetaInfos().getParametersToReturn().add(Pair.of(parameterStatement, returnStatement));
    }

    /**
     * The parts of the table its conditions and its actions are written in, each holding what that column was
     * declared to hold.
     *
     * <p>A decision table declares what a condition takes and what an action gives once, in its headers, and
     * every rule written under them holds it. So a part answers for a rule nobody has written yet: the cells of
     * a row laid down under the last rule, and the cells of a table whose conditions are declared and which
     * holds no rules at all.
     *
     * <p>Where rules have been written, a part is the cells they were written in, reaching on past the table's
     * edge — down a Rules table, across a table written the other way round, down and across the cells a
     * lookup's rules meet in. Where none have, it is read from the title each column is shown under.
     */
    @Override
    public List<TableArea> getAreas() {
        try {
            return areas();
        } catch (Exception e) {
            // Something unexpected is occurred. Work without the areas.
            log.error(e.getMessage(), e);
            return List.of();
        }
    }

    private List<TableArea> areas() {
        var decisionTable = getDecisionTable();
        if (decisionTable == null) {
            return List.of();
        }
        var rows = new ArrayList<FunctionalRow>();
        if (decisionTable.getConditionRows() != null) {
            for (IBaseCondition condition : decisionTable.getConditionRows()) {
                rows.add((FunctionalRow) condition);
            }
        }
        if (decisionTable.getActionRows() != null) {
            for (IBaseAction action : decisionTable.getActionRows()) {
                rows.add((FunctionalRow) action);
            }
        }
        var region = getTableSyntaxNode().getGridTable().getRegion();
        // Where a lookup's rules run across as well as down, the corner they turn at bounds what is left.
        var corner = lookupCorner(rows);
        // Which way the rules of the table run is the table's own to say, and it says so whether or not any
        // have been written; see DTInfo.
        var info = decisionTable.getDtInfo();
        var across = info != null && info.isTransposed();
        var areas = new ArrayList<TableArea>();
        for (FunctionalRow funcRow : rows) {
            areas.addAll(areasOf(funcRow, region, corner, across));
        }
        return areas;
    }

    /**
     * The parts one condition or one action is written in, each reaching on past the last rule written.
     *
     * <p>A condition holds one cell of every rule, so its part runs the way the table grows and is as wide as
     * the cell it holds. A horizontal condition runs across whichever way the table is written, and the cells a
     * lookup's rules meet in run both ways.
     */
    private static List<TableArea> areasOf(FunctionalRow funcRow, IGridRegion region, Box corner, boolean across) {
        var params = funcRow.getParams();
        if (params == null || params.length == 0) {
            return List.of();
        }
        var horizontal = isHorizontal(funcRow);
        // The cells a lookup's rules meet in are added to by a rule written either way.
        var meeting = corner != null && funcRow instanceof IBaseAction;
        var right = meeting || horizontal || across;
        var down = meeting || !right;
        // Where each parameter of the row is written, as the first rule written puts it. A part runs on past
        // the last rule the way the table grows, so only the rule the table begins with settles where it is —
        // and asking the rest would walk every value of every rule to throw the answer away.
        var boxes = new Box[params.length];
        eachValue(funcRow, true, (i, param, storageValue, cells) -> {
            if (param != null) {
                boxes[i] = Box.around(boxes[i], cells.getCell(0, 0));
            }
        });
        var areas = new ArrayList<TableArea>();
        for (var i = 0; i < params.length; i++) {
            if (params[i] != null && boxes[i] != null) {
                areas.add(boxes[i].reaching(region, down, right, metaInfoOf(params[i])));
            }
        }
        // A table nobody has written a rule in yet is read from the titles its columns are shown under.
        return areas.isEmpty() ? declared(funcRow, params, region, corner, horizontal, meeting) : areas;
    }

    /**
     * Where a condition or an action would be written in a table that holds no rules, read from its titles.
     *
     * <p>A horizontal condition is written across the row its title stands in; anything else is written down
     * the column of its title, under the last of the horizontal conditions where the table has any. An action
     * of a lookup has no title of its own — the cells its rules meet in begin where the horizontal conditions
     * do and run down and across from there.
     */
    private static List<TableArea> declared(FunctionalRow funcRow, IParameterDeclaration[] params,
            IGridRegion region, Box corner, boolean horizontal, boolean meeting) {
        if (meeting) {
            // A parameter that did not bind says nothing about what its cells hold.
            return params[0] == null ? List.of()
                    : List.of(corner.under().reaching(region, true, true, metaInfoOf(params[0])));
        }
        var titles = funcRow.getPresentationTable();
        if (titles == null) {
            return List.of();
        }
        var areas = new ArrayList<TableArea>();
        for (var i = 0; i < titles.getHeight(); i++) {
            // A column shown under one title holds one type, however many parameters the condition reads it as.
            var param = params[Math.min(i, params.length - 1)];
            if (param == null) {
                continue;
            }
            var title = Box.around(null, titles.getCell(0, i));
            // Across the row the title stands in, or down its column — under the titles, or under the
            // horizontal conditions where the table turns a corner.
            var part = horizontal ? title : (corner == null ? title : corner).under().sameColumnsAs(title);
            areas.add(part.reaching(region, !horizontal, horizontal, metaInfoOf(param)));
        }
        return areas;
    }

    /** Where the horizontal conditions of a lookup stand, or {@code null} where the table has none. */
    private static Box lookupCorner(List<FunctionalRow> rows) {
        Box corner = null;
        for (FunctionalRow funcRow : rows) {
            if (!isHorizontal(funcRow)) {
                continue;
            }
            var titles = funcRow.getPresentationTable();
            if (titles == null) {
                continue;
            }
            for (var i = 0; i < titles.getHeight(); i++) {
                corner = Box.around(corner, titles.getCell(0, i));
            }
        }
        return corner;
    }

    /** Whether the rules of this condition run across the table rather than down it. */
    private static boolean isHorizontal(FunctionalRow funcRow) {
        return funcRow.getName() != null && funcRow.getName()
                .startsWith(DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
    }

    /**
     * The cells something is written in, by the workbook's own rows and columns.
     *
     * @param top    the first row, the last one being {@code bottom}
     * @param left   the first column, the last one being {@code right}
     */
    private record Box(int top, int left, int bottom, int right) {

        /** The box holding both what was there and the cell, the cell alone where nothing was. */
        static Box around(Box was, ICell cell) {
            var merged = cell.getAbsoluteRegion();
            var added = new Box(merged.getTop(), merged.getLeft(), merged.getBottom(), merged.getRight());
            return was == null ? added
                    : new Box(Math.min(was.top, added.top), Math.min(was.left, added.left),
                            Math.max(was.bottom, added.bottom), Math.max(was.right, added.right));
        }

        /** The line lying just under this box, as wide as it is. */
        Box under() {
            return new Box(bottom + 1, left, bottom + 1, right);
        }

        /** This box moved into the columns of another, keeping the rows it has. */
        Box sameColumnsAs(Box other) {
            return new Box(top, other.left, bottom, other.right);
        }

        /** The part of the table this box covers, running on past its last cell the ways it is asked to. */
        TableArea reaching(IGridRegion region, boolean down, boolean right, CellMetaInfo metaInfo) {
            return new TableArea(top - region.getTop(),
                    left - region.getLeft(),
                    down ? TableArea.TO_THE_END : bottom - top + 1,
                    right ? TableArea.TO_THE_END : this.right - left + 1,
                    metaInfo);
        }
    }

    private void saveValueMetaInfo(FunctionalRow funcRow, IGridRegion region) {
        eachValue(funcRow, false, (i, param, storageValue, cells) -> {
            if (storageValue instanceof CompositeMethod) {
                addMetaInfoForCompositeMethod(region, cells, 0, 0, storageValue);
            } else if (storageValue instanceof ArrayHolder) {
                addMetaInfoForArrayHolder(region, cells, storageValue);
            } else if (param != null) {
                // Written through the overload that keeps the node usages a cell was already read with.
                var metaInfo = metaInfoOf(param);
                var cell = cells.getCell(0, 0);
                setPreparedMetaInfo(cell.getAbsoluteRow(),
                        cell.getAbsoluteColumn(),
                        metaInfo.getDataType(),
                        metaInfo.isMultiValue());
            }
        });
    }

    /**
     * Walks every value the row holds, one parameter of one rule at a time.
     *
     * <p>Lookup tables are transformed to Rules tables so we cannot predict real column and row of a cell. In
     * current implementation we run through all of them and if it's current row and cell.
     *
     * @param firstRuleOnly reads the rule the table begins with and stops, for a caller that only needs to
     *                      know where the row is written rather than what every rule of it holds
     */
    private static void eachValue(FunctionalRow funcRow, boolean firstRuleOnly, ValueReader reader) {
        var rules = firstRuleOnly ? Math.min(1, funcRow.nValues()) : funcRow.nValues();
        for (var c = 0; c < rules; c++) {
            // In the case of errors params will be null
            var params = funcRow.getParams();
            int paramsCount = params == null ? 0 : params.length;
            var valueCell = funcRow.getValueCell(c);
            var paramTable = funcRow.getParamsTable();
            var offsetByParamTable = 0;
            var offsetByValueCell = 0;
            var j = 0;
            for (var i = 0; i < paramsCount; i++) {
                offsetByParamTable = offsetByParamTable + (paramTable
                        .isNormalOrientation() ? paramTable.getRow(i).getSource().getWidth()
                        : paramTable.getRow(i).getSource().getHeight());
                var storageValue = funcRow.getStorageValue(i, c);
                var d = 0;
                while (offsetByValueCell < offsetByParamTable) {
                    offsetByValueCell = offsetByValueCell + (valueCell
                            .isNormalOrientation() ? valueCell.getRow(j).getSource().getWidth()
                            : valueCell.getRow(j).getSource().getHeight());
                    d++;
                    j++;
                }
                if (d > 0) {
                    ILogicalTable cells;
                    if (valueCell.isNormalOrientation()) {
                        cells = valueCell.getSubtable(j - d, 0, d, valueCell.getHeight());
                    } else {
                        cells = valueCell.getSubtable(0, j - d, valueCell.getWidth(), d);
                    }
                    reader.read(i, params[i], storageValue, cells);
                }
            }
        }
    }

    /** What a parameter's cell holds: the type it was declared with, and whether one cell holds many of them. */
    private static CellMetaInfo metaInfoOf(IParameterDeclaration param) {
        var type = param.getType();
        if (type.isArray()) {
            return new CellMetaInfo(type.getAggregateInfo().getComponentType(type), true);
        }
        return new CellMetaInfo(type, false);
    }

    /** Reads one parameter of one rule: what it was declared as, what it holds, and the cells it is written in. */
    @FunctionalInterface
    private interface ValueReader {
        void read(int paramIndex, IParameterDeclaration param, Object storageValue, ILogicalTable cells);
    }

    private void addMetaInfoForCompositeMethod(IGridRegion region,
                                               ILogicalTable valueCell,
                                               int i,
                                               int j,
                                               Object storageValue) {
        var cell = valueCell.getCell(j, i); // See EPBDS-7774 for an example when "i" is needed
        var row = cell.getAbsoluteRow();
        var col = cell.getAbsoluteColumn();
        if (IGridRegion.Tool.contains(region, col, row)) {
            // Some expression
            var stringValue = cell.getStringValue();
            var startIndex = stringValue.indexOf('=') + 1;
            var nodeUsages = MetaInfoReaderUtils
                    .getNodeUsages((CompositeMethod) storageValue, stringValue, startIndex);
            setPreparedMetaInfo(row, col, new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages));
        }
    }

    private void addMetaInfoForArrayHolder(IGridRegion region, ILogicalTable valueCell, Object storageValue) {
        var arrayHolder = (ArrayHolder) storageValue;
        if (arrayHolder.is2DimArray()) {
            var values = arrayHolder.get2DimValues();
            for (var i = 0; i < values.length; i++) {
                for (var j = 0; j < values[i].length; j++) {
                    if (values[i][j] instanceof CompositeMethod) {
                        addMetaInfoForCompositeMethod(region, valueCell, j, i, values[i][j]);
                    }
                }
            }
        } else {
            var values = arrayHolder.getValues();
            for (var i = 0; i < values.length; i++) {
                if (values[i] instanceof CompositeMethod) {
                    if (valueCell.getHeight() > 1) {
                        addMetaInfoForCompositeMethod(region, valueCell, i, 0, values[i]);
                    } else {
                        addMetaInfoForCompositeMethod(region, valueCell, 0, i, values[i]);
                    }
                }
            }
        }

    }

    private void saveExpressionMetaInfo(FunctionalRow funcRow, IGridRegion region) {
        // Condition/Action code (expression)
        var codeCell = funcRow.getCodeTable().getCell(0, 0);
        var row = codeCell.getAbsoluteRow();
        var col = codeCell.getAbsoluteColumn();
        if (IGridRegion.Tool.contains(region, col, row)) {
            var metaInfoList = MetaInfoReaderUtils.getMetaInfo(funcRow.getSourceCodeModule(),
                    funcRow.getMethod());
            // Decision table always contains 1 meta info
            setPreparedMetaInfo(row, col, metaInfoList.getFirst());
        }

        // Condition/Action type definition
        var paramsTable = funcRow.getParamsTable();
        // In the case of errors params will be null
        var params = funcRow.getParams();
        if (params != null) {
            var i = 0;
            for (IParameterDeclaration param : params) {
                if (param != null) {
                    var paramCell = paramsTable.getCell(0, i);
                    row = paramCell.getAbsoluteRow();
                    col = paramCell.getAbsoluteColumn();
                    if (IGridRegion.Tool.contains(region, col, row)) {
                        setPreparedMetaInfo(row, col, getMetaInfo(paramsTable, param.getType()));
                    }
                }
                i++;
            }
        }
    }

    protected CellMetaInfo getMetaInfo(ILogicalTable paramsTable, IOpenClass type) {
        var typeForLink = type;
        while (typeForLink.getMetaInfo() == null && typeForLink.isArray()) {
            typeForLink = typeForLink.getComponentClass();
        }

        var table = paramsTable.getRow(0);
        if (table != null) {
            var source = new GridCellSourceCodeModule(table.getSource());
            IdentifierNode[] paramNodes;
            try {
                paramNodes = Tokenizer.tokenize(source, "[] \n\r");
            } catch (OpenLCompilationException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            if (paramNodes.length > 0) {
                var metaInfo = typeForLink.getMetaInfo();
                if (metaInfo != null) {
                    var nodeUsage = new SimpleNodeUsage(paramNodes[0],
                            metaInfo.getDisplayName(INamedThing.SHORT),
                            metaInfo.getSourceUrl(),
                            typeForLink,
                            NodeType.DATATYPE);
                    return new CellMetaInfo(JavaOpenClass.STRING, false, List.of(nodeUsage));
                }
            }
        }

        return null;
    }

    private DecisionTable getDecisionTable() {
        if (decisionTable != null) {
            return decisionTable;
        }
        return getBoundNode().getDecisionTable();
    }

    private CellMetaInfo getPreparedMetaInfo(int row, int col) {
        if (preparedMetaInfos == null) {
            if (getTableSyntaxNode().getGridTable().getGrid() instanceof CompositeGrid) {
                for (IGridTable gridTable : ((CompositeGrid) getTableSyntaxNode().getGridTable().getGrid())
                        .getGridTables()) {
                    prepare(gridTable.getRegion());
                }
            } else {
                prepare(getTableSyntaxNode().getGridTable().getRegion());
            }
        }
        return preparedMetaInfos.get(CellKey.CellKeyFactory.getCellKey(col, row));
    }

    private void setPreparedMetaInfo(int row, int col, CellMetaInfo metaInfo) {
        if (metaInfo != null) {
            preparedMetaInfos.put(CellKey.CellKeyFactory.getCellKey(col, row), metaInfo);
        }
    }

    private void setPreparedMetaInfo(int row, int col, IOpenClass type, boolean multiValue) {
        var metaInfo = new CellMetaInfo(type, multiValue);
        var previous = getPreparedMetaInfo(row, col);
        if (previous != null && previous.getUsedNodes() != null) {
            metaInfo.setUsedNodes(previous.getUsedNodes());
        }
        setPreparedMetaInfo(row, col, metaInfo);
    }

    private static class ReturnMetaInfo {
        @Getter
        final String details;
        @Getter
        final String uri;

        public ReturnMetaInfo(String details, String uri) {
            this.details = details;
            this.uri = uri;
        }

    }

    private static class HeaderMetaInfo {
        @Getter
        String header;
        @Getter
        String[] parameterNames;
        String statement;
        @Getter
        IOpenClass[] columnTypes;
        @Getter
        String additionalDetails;
        @Getter
        String url;
        @Getter
        boolean vertical;

        public HeaderMetaInfo(String headerName,
                              String[] parameterNames,
                              String conditionStatement,
                              IOpenClass[] columnTypes,
                              String url,
                              String additionalDetails,
                              boolean vertical) {
            if (parameterNames != null && columnTypes != null && parameterNames.length != columnTypes.length) {
                throw new IllegalArgumentException();
            }
            this.header = headerName;
            this.parameterNames = parameterNames;
            this.statement = conditionStatement;
            this.columnTypes = columnTypes;
            this.additionalDetails = additionalDetails;
            this.url = url;
            this.vertical = vertical;
        }

        public String getConditionStatement() {
            return statement;
        }
    }
}
