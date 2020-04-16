package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableBoundNode;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecisionTableMetaInfoReader extends AMethodMetaInfoReader<DecisionTableBoundNode> {
    private final Logger log = LoggerFactory.getLogger(DecisionTableMetaInfoReader.class);
    private final DecisionTable decisionTable;
    private CellMetaInfo[][] preparedMetaInfos;
    private int top;
    private int left;
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
        private final Map<CellKey, HeaderMetaInfo> simpleRulesConditionMap = new HashMap<>();

        /**
         * Map for action cells in header to parameter index
         */
        private final Map<CellKey, HeaderMetaInfo> simpleRulesActionMap = new HashMap<>();

        /**
         * Map for compound return column descriptions in SimpleRules header
         */
        private final Map<CellKey, ReturnMetaInfo> simpleRulesReturnMap = new HashMap<>();

        /**
         * List for inputParameter mapping details for smart dt
         */
        private final List<Pair<String, String>> inputParametersToReturn = new ArrayList<>();

        private final List<CellKey> unmatchedColumns = new ArrayList<>();

        public Map<CellKey, HeaderMetaInfo> getSimpleRulesConditionMap() {
            return simpleRulesConditionMap;
        }

        public Map<CellKey, HeaderMetaInfo> getSimpleRulesActionMap() {
            return simpleRulesActionMap;
        }

        public Map<CellKey, ReturnMetaInfo> getSimpleRulesReturnMap() {
            return simpleRulesReturnMap;
        }

        public List<Pair<String, String>> getInputParametersToReturn() {
            return inputParametersToReturn;
        }

        public List<CellKey> getUnmatchedColumns() {
            return unmatchedColumns;
        }

        public void merge(MetaInfoHolder metaInfoHolder) {
            if (metaInfoHolder == null) {
                return;
            }
            simpleRulesConditionMap.putAll(metaInfoHolder.simpleRulesConditionMap);
            simpleRulesActionMap.putAll(metaInfoHolder.simpleRulesActionMap);
            simpleRulesReturnMap.putAll(metaInfoHolder.simpleRulesReturnMap);
            inputParametersToReturn.addAll(metaInfoHolder.inputParametersToReturn);
            unmatchedColumns.addAll(metaInfoHolder.unmatchedColumns);
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
        final List<Pair<String, String>> inputParametersToReturn = getMetaInfos().getInputParametersToReturn();
        if (inputParametersToReturn.isEmpty()) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            inputParametersToReturn.sort(Map.Entry.comparingByKey());
            for (Pair<String, String> p : inputParametersToReturn) {
                if (sb.length() > 0) {
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
            top = region.getTop();
            left = region.getLeft();
            preparedMetaInfos = new CellMetaInfo[region.getBottom() - top + 1][region.getRight() - left + 1];

            DecisionTable decisionTable = getDecisionTable();

            saveSimpleRulesMetaInfo(region);
            saveCompoundReturnColumn(region);

            IBaseCondition[] conditionRows = decisionTable.getConditionRows();
            IBaseAction[] actionRows = decisionTable.getActionRows();

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
                    FunctionalRow funcRow = (FunctionalRow) condition;
                    saveValueMetaInfo(funcRow, region);
                }
            }
            if (actionRows != null) {
                // Action values
                for (IBaseAction action : actionRows) {
                    FunctionalRow funcRow = (FunctionalRow) action;
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
            HeaderMetaInfo headerMetaInfo,
            IGridRegion region,
            Function<HeaderMetaInfo, String> headerToString) {

        int row = key.getRow();
        int col = key.getColumn();
        if (!IGridRegion.Tool.contains(region, col, row)) {
            return;
        }

        // SimpleRules or SimpleLookup
        IGrid grid = getTableSyntaxNode().getGridTable().getGrid();
        String cellValue = grid.getCell(col, row).getStringValue();
        if (StringUtils.isBlank(cellValue)) {
            return;
        }

        String text = headerToString.apply(headerMetaInfo);
        SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
            cellValue.length() - 1,
            text,
            headerMetaInfo.getUrl(),
            headerMetaInfo.getUrl() != null ? NodeType.OTHERUNDERLINED : NodeType.OTHER);
        setPreparedMetaInfo(row,
            col,
            new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(simpleNodeUsage)));
    }

    private String buildStringForCondition(HeaderMetaInfo headerMetaInfo) {
        String[] parameterNames = headerMetaInfo.getParameterNames();
        String header = headerMetaInfo.getHeader();
        String statement = headerMetaInfo.getConditionStatement();
        IOpenClass[] columnTypes = headerMetaInfo.getColumnTypes();

        StringBuilder sb = new StringBuilder();
        sb.append("Condition: ").append(header);
        if (!StringUtils.isEmpty(statement)) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("Expression: ").append(statement.replaceAll("\n", StringUtils.SPACE));
        }
        if (!StringUtils.isEmpty(headerMetaInfo.getAdditionalDetails())) {
            if (sb.length() > 0) {
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
        int i = 0;
        if (sb.length() > 0) {
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
            if (parameterNames != null && parameterNames[i] != null) {
                sb.append(StringUtils.SPACE).append(parameterNames[i]);
            }
            i++;
        }
    }

    private String buildStringForAction(HeaderMetaInfo headerMetaInfo) {
        String[] parameterNames = headerMetaInfo.getParameterNames();
        String header = headerMetaInfo.getHeader();
        String statement = headerMetaInfo.getConditionStatement();
        IOpenClass[] columnTypes = headerMetaInfo.getColumnTypes();
        StringBuilder sb = new StringBuilder();
        sb.append("Action: ").append(header);
        if (!StringUtils.isEmpty(statement)) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("Expression: ").append(statement.replaceAll("\n", StringUtils.SPACE));
        }
        if (!StringUtils.isEmpty(headerMetaInfo.getAdditionalDetails())) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(headerMetaInfo.getAdditionalDetails());
        }
        appendParameters(sb, parameterNames, columnTypes);
        return sb.toString();
    }

    private void saveSimpleRulesMetaInfo(IGridRegion region) {
        final Map<CellKey, HeaderMetaInfo> simpleRulesConditionMap = getMetaInfos().getSimpleRulesConditionMap();
        for (Map.Entry<CellKey, HeaderMetaInfo> entry : simpleRulesConditionMap.entrySet()) {
            setMetaInfo(entry.getKey(), entry.getValue(), region, this::buildStringForCondition);
        }
        final Map<CellKey, HeaderMetaInfo> simpleRulesActionMap = getMetaInfos().getSimpleRulesActionMap();
        for (Map.Entry<CellKey, HeaderMetaInfo> entry : simpleRulesActionMap.entrySet()) {
            setMetaInfo(entry.getKey(), entry.getValue(), region, this::buildStringForAction);
        }
        final List<CellKey> unmatchedColumns = getMetaInfos().getUnmatchedColumns();
        for (CellKey cellKey : unmatchedColumns) {
            setMetaInfoForUnmatchedColumn(cellKey);
        }
    }

    private void setMetaInfoForUnmatchedColumn(CellKey cellKey) {
        IGrid grid = getTableSyntaxNode().getGridTable().getGrid();
        String cellValue = grid.getCell(cellKey.getColumn(), cellKey.getRow()).getStringValue();
        SimpleNodeUsage nodeUsage = new SimpleNodeUsage(0,
            cellValue.length() - 1,
            "Unmatched column",
            null,
            NodeType.OTHER);
        setPreparedMetaInfo(cellKey.getRow(),
            cellKey.getColumn(),
            new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(nodeUsage)));
    }

    private void saveCompoundReturnColumn(IGridRegion region) {
        final Map<CellKey, ReturnMetaInfo> simpleRulesReturnDescriptions = getMetaInfos().getSimpleRulesReturnMap();
        for (Map.Entry<CellKey, ReturnMetaInfo> entry : simpleRulesReturnDescriptions.entrySet()) {
            CellKey key = entry.getKey();
            int row = key.getRow();
            int col = key.getColumn();
            if (!IGridRegion.Tool.contains(region, col, row)) {
                continue;
            }

            ICell cell = getTableSyntaxNode().getGridTable().getGrid().getCell(col, row);
            String stringValue = cell.getStringValue();

            if (StringUtils.isBlank(stringValue)) {
                continue;
            }
            ReturnMetaInfo returnMetaInfo = entry.getValue();
            SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
                stringValue.length() - 1,
                returnMetaInfo.getDetails(),
                returnMetaInfo.getUri(),
                returnMetaInfo.getUri() != null ? NodeType.OTHERUNDERLINED : NodeType.OTHER);
            CellMetaInfo metaInfo = new CellMetaInfo(JavaOpenClass.STRING,
                false,
                Collections.singletonList(simpleNodeUsage));
            setPreparedMetaInfo(row, col, metaInfo);
        }
    }

    public void addSimpleRulesCondition(int row,
            int col,
            String header,
            String[] parameterNames,
            String statement,
            IOpenClass[] columnTypes,
            String url,
            String additionalDetails) {
        getMetaInfos().getSimpleRulesConditionMap()
            .put(CellKey.CellKeyFactory.getCellKey(col, row),
                new HeaderMetaInfo(header, parameterNames, statement, columnTypes, url, additionalDetails));
    }

    public void addDescription(int row, int col) {
        getMetaInfos().getUnmatchedColumns().add(CellKey.CellKeyFactory.getCellKey(col, row));
    }

    public void addSimpleRulesAction(int row,
            int col,
            String header,
            String[] parameterNames,
            String statement,
            IOpenClass[] columnTypes,
            String url,
            String additionalInfo) {
        getMetaInfos().getSimpleRulesActionMap()
            .put(CellKey.CellKeyFactory.getCellKey(col, row),
                new HeaderMetaInfo(header, parameterNames, statement, columnTypes, url, additionalInfo));
    }

    public void addSimpleRulesReturn(int row, int col, String details, String uri) {
        getMetaInfos().getSimpleRulesReturnMap()
            .put(CellKey.CellKeyFactory.getCellKey(col, row), new ReturnMetaInfo(details, uri));
    }

    public void addInputParametersToReturn(String statementInInputParameters, String statementInReturn) {
        getMetaInfos().getInputParametersToReturn().add(Pair.of(statementInInputParameters, statementInReturn));
    }

    private void saveValueMetaInfo(FunctionalRow funcRow, IGridRegion region) {
        // Lookup tables are transformed to Rules tables so we cannot predict real column and row of a cell.
        // In current implementation we run through all of them and if it's current row and cell.
        for (int c = 0; c < funcRow.nValues(); c++) {
            // In the case of errors params will be null
            IParameterDeclaration[] params = funcRow.getParams();
            int paramsCount = params == null ? 0 : params.length;

            ILogicalTable valueCell = funcRow.getValueCell(c);

            for (int i = 0; i < paramsCount; i++) {
                ICell cell = valueCell.getCell(0, i); // See EPBDS-7774 for an example when "i" is needed
                int row = cell.getAbsoluteRow();
                int col = cell.getAbsoluteColumn();

                if (!IGridRegion.Tool.contains(region, col, row)) {
                    continue;
                }

                Object storageValue = funcRow.getStorageValue(i, c);
                if (storageValue instanceof CompositeMethod) {
                    // Some expression
                    String stringValue = cell.getStringValue();
                    int startIndex = stringValue.indexOf('=') + 1;
                    List<NodeUsage> nodeUsages = MetaInfoReaderUtils
                        .getNodeUsages((CompositeMethod) storageValue, stringValue.substring(startIndex), startIndex);
                    setPreparedMetaInfo(row, col, new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages));
                    continue;
                }

                IParameterDeclaration param = params[i];
                if (param == null) {
                    continue;
                }
                IOpenClass type = param.getType();
                boolean multiValue = false;
                if (type.isArray()) {
                    multiValue = true;
                    type = type.getAggregateInfo().getComponentType(type);
                }
                setPreparedMetaInfo(row, col, type, multiValue);
            }
        }
    }

    private void saveExpressionMetaInfo(FunctionalRow funcRow, IGridRegion region) {
        // Condition/Action code (expression)
        ICell codeCell = funcRow.getCodeTable().getCell(0, 0);
        int row = codeCell.getAbsoluteRow();
        int col = codeCell.getAbsoluteColumn();
        if (IGridRegion.Tool.contains(region, col, row)) {
            List<CellMetaInfo> metaInfoList = MetaInfoReaderUtils.getMetaInfo(funcRow.getSourceCodeModule(),
                funcRow.getMethod());
            // Decision table always contains 1 meta info
            setPreparedMetaInfo(row, col, metaInfoList.get(0));
        }

        // Condition/Action type definition
        ILogicalTable paramsTable = funcRow.getParamsTable();
        // In the case of errors params will be null
        IParameterDeclaration[] params = funcRow.getParams();
        if (params != null && params.length > 0 && params[0] != null) {
            IParameterDeclaration param = params[0];
            ICell paramCell = paramsTable.getCell(0, 0);
            row = paramCell.getAbsoluteRow();
            col = paramCell.getAbsoluteColumn();

            if (IGridRegion.Tool.contains(region, col, row)) {
                setPreparedMetaInfo(row, col, getMetaInfo(paramsTable, param.getType()));
            }
        }
    }

    protected CellMetaInfo getMetaInfo(ILogicalTable paramsTable, IOpenClass type) {
        IOpenClass typeForLink = type;
        while (typeForLink.getMetaInfo() == null && typeForLink.isArray()) {
            typeForLink = typeForLink.getComponentClass();
        }

        ILogicalTable table = paramsTable.getRow(0);
        if (table != null) {
            GridCellSourceCodeModule source = new GridCellSourceCodeModule(table.getSource());
            IdentifierNode[] paramNodes;
            try {
                paramNodes = Tokenizer.tokenize(source, "[] \n\r");
            } catch (OpenLCompilationException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            if (paramNodes.length > 0) {
                IMetaInfo metaInfo = typeForLink.getMetaInfo();
                if (metaInfo != null) {
                    SimpleNodeUsage nodeUsage = new SimpleNodeUsage(paramNodes[0],
                        metaInfo.getDisplayName(INamedThing.SHORT),
                        metaInfo.getSourceUrl(),
                        NodeType.DATATYPE);

                    return new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(nodeUsage));
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
            prepare(getTableSyntaxNode().getGridTable().getRegion());
        }
        int r = row - top;
        int c = col - left;
        if (r < 0 || r >= preparedMetaInfos.length || c < 0 || c >= preparedMetaInfos[0].length) {
            return null;
        }
        return preparedMetaInfos[r][c];
    }

    private void setPreparedMetaInfo(int row, int col, CellMetaInfo metaInfo) {
        preparedMetaInfos[row - top][col - left] = metaInfo;
    }

    private void setPreparedMetaInfo(int row, int col, IOpenClass type, boolean multiValue) {
        CellMetaInfo metaInfo = new CellMetaInfo(type, multiValue);
        CellMetaInfo previous = preparedMetaInfos[row - top][col - left];
        if (previous != null && previous.getUsedNodes() != null) {
            metaInfo.setUsedNodes(previous.getUsedNodes());
        }
        setPreparedMetaInfo(row, col, metaInfo);
    }

    private static class ReturnMetaInfo {
        String details;
        String uri;

        public ReturnMetaInfo(String details, String uri) {
            super();
            this.details = details;
            this.uri = uri;
        }

        public String getDetails() {
            return details;
        }

        public String getUri() {
            return uri;
        }

    }

    private static class HeaderMetaInfo {
        String header;
        String[] parameterNames;
        String statement;
        IOpenClass[] columnTypes;
        String additionalDetails;
        String url;

        public HeaderMetaInfo(String headerName,
                String[] parameterNames,
                String conditionStatement,
                IOpenClass[] columnTypes,
                String url,
                String additionalDetails) {
            if (parameterNames != null && columnTypes != null && parameterNames.length != columnTypes.length) {
                throw new IllegalArgumentException();
            }
            this.header = headerName;
            this.parameterNames = parameterNames;
            this.statement = conditionStatement;
            this.columnTypes = columnTypes;
            this.additionalDetails = additionalDetails;
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public String getAdditionalDetails() {
            return additionalDetails;
        }

        public String getHeader() {
            return header;
        }

        public String[] getParameterNames() {
            return parameterNames;
        }

        public String getConditionStatement() {
            return statement;
        }

        public IOpenClass[] getColumnTypes() {
            return columnTypes;
        }
    }
}
