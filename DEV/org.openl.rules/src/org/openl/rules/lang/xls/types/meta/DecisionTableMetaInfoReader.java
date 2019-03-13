package org.openl.rules.lang.xls.types.meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableBoundNode;
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
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecisionTableMetaInfoReader extends AMethodMetaInfoReader<DecisionTableBoundNode> {
    private final Logger log = LoggerFactory.getLogger(DecisionTableMetaInfoReader.class);
    private final DecisionTable decisionTable;

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
    private final Map<CellKey, String> simpleRulesReturnDescriptions = new HashMap<>();

    private CellMetaInfo[][] preparedMetaInfos;
    private int top;
    private int left;

    public DecisionTableMetaInfoReader(DecisionTableBoundNode boundNode) {
        this(boundNode, null);
    }

    public DecisionTableMetaInfoReader(DecisionTableBoundNode boundNode, DecisionTable decisionTable) {
        super(boundNode);
        this.decisionTable = decisionTable;
    }

    @Override
    public void prepare(IGridRegion region) {
        try {
            top = region.getTop();
            left = region.getLeft();
            preparedMetaInfos = new CellMetaInfo[region.getBottom() - top + 1][region.getRight() - left + 1];

            DecisionTable decisionTable = getDecisionTable();

            IBaseCondition[] conditionRows = decisionTable.getConditionRows();
            IBaseAction[] actionRows = decisionTable.getActionRows();

            saveSimpleRulesMetaInfo(decisionTable, region);
            saveCompoundReturnColumn(region);

            if (conditionRows != null) {
                // Condition description
                for (IBaseCondition conditionRow : conditionRows) {
                    saveDescriptionMetaInfo((FunctionalRow) conditionRow, region);
                }

                // Condition values
                for (IBaseCondition condition : conditionRows) {
                    FunctionalRow funcRow = (FunctionalRow) condition;
                    saveValueMetaInfo(funcRow, region);
                }
            }

            if (actionRows != null) {
                // Action description
                for (IBaseAction action : actionRows) {
                    saveDescriptionMetaInfo((FunctionalRow) action, region);
                }

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
    
    public static <T> String toString(T[] elements, Function<T, String> func) {
        if (elements == null || elements.length == 0) {
            return null;
        }
        if (elements.length == 1) {
            return func.apply(elements[0]);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(func.apply(elements[0]));
            for (int i = 1; i < elements.length; i++) {
                sb.append(", ");
                sb.append(func.apply(elements[i]));
            }
            sb.append("]");
            return sb.toString();
        }
    }
    
    private void setMetaInfo(CellKey key, HeaderMetaInfo headerMetaInfo, IGridRegion region, Function<HeaderMetaInfo, String> headerToString) {
 
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
                null,
                NodeType.OTHER);
        setPreparedMetaInfo(row,
                col,
                new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(simpleNodeUsage)));
    }

    private String buildStringForCondition(HeaderMetaInfo headerMetaInfo) {
        String[] parameterNames = headerMetaInfo.getParameterNames();
        String headerName = headerMetaInfo.getConditionName();
        String statement = headerMetaInfo.getConditionStatement();
        IOpenClass[] columnTypes = headerMetaInfo.getColumnTypes();
        
        String value;
        String valueForColumnTypes = toString(columnTypes, e -> e.getDisplayName(INamedThing.SHORT));
        if (parameterNames != null && parameterNames.length > 0 && headerName != null && statement != null) {
            if (parameterNames.length > 1) {
                String textForParameterNames = toString(parameterNames, e -> e);
                value = String.format("Parameters %s of condition %s with expression %s: %s", textForParameterNames, headerName, statement, valueForColumnTypes);
            }else {
                value = String.format("Parameter %s of condition %s with expression %s: %s", parameterNames[0], headerName, statement, valueForColumnTypes);
            }
        } else if (headerName != null && statement != null) {
            value = String.format("Condition %s with expression %s: %s", headerName, statement, valueForColumnTypes);
        } else {
            value = String.format("Condition for %s: %s", statement, valueForColumnTypes);
        }
        return value;
    }

    private String buildStringForAction(HeaderMetaInfo headerMetaInfo) {
        String[] parameterNames = headerMetaInfo.getParameterNames();
        String headerName = headerMetaInfo.getConditionName();
        String statement = headerMetaInfo.getConditionStatement();
        IOpenClass[] columnTypes = headerMetaInfo.getColumnTypes();
        
        String value;
        String valueForColumnTypes = toString(columnTypes, e -> e.getDisplayName(INamedThing.SHORT));
        if (parameterNames != null && parameterNames.length > 0 && headerName != null && statement != null) {
            if (parameterNames.length > 1) {
                String textForParameterNames = toString(parameterNames, e -> e);
                value = String.format("Parameters %s of action %s with expression %s: %s", textForParameterNames, headerName, statement, valueForColumnTypes);
            }else {
                value = String.format("Parameter %s of action %s with expression %s: %s", parameterNames[0], headerName, statement, valueForColumnTypes);
            }
        } else if (headerName != null && statement != null) {
            value = String.format("Action %s with expression %s: %s", headerName, statement, valueForColumnTypes);
        } else {
            value = String.format("Action for %s: %s", statement, valueForColumnTypes);
        }
        return value;
    }


    private void saveSimpleRulesMetaInfo(DecisionTable decisionTable, IGridRegion region) {
        for (Map.Entry<CellKey, HeaderMetaInfo> entry : simpleRulesConditionMap.entrySet()) {
            setMetaInfo(entry.getKey(), entry.getValue(), region, this::buildStringForCondition);
        }
        for (Map.Entry<CellKey, HeaderMetaInfo> entry : simpleRulesActionMap.entrySet()) {
            setMetaInfo(entry.getKey(), entry.getValue(), region, this::buildStringForAction);
        }
    }

    private void saveCompoundReturnColumn(IGridRegion region) {
        for (Map.Entry<CellKey, String> entry : simpleRulesReturnDescriptions.entrySet()) {
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
            
            SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
                stringValue.length() - 1,
                entry.getValue(),
                null,
                NodeType.OTHER);
            CellMetaInfo metaInfo = new CellMetaInfo(JavaOpenClass.STRING,
                false,
                Collections.singletonList(simpleNodeUsage));
            setPreparedMetaInfo(row, col, metaInfo);
        }
    }

    public void addSimpleRulesCondition(int row, int col, String conditionName, String[] parameterNames, String conditionStatement, IOpenClass[] columnTypes) {
        simpleRulesConditionMap.put(CellKey.CellKeyFactory.getCellKey(col, row), new HeaderMetaInfo(conditionName, parameterNames, conditionStatement, columnTypes));
    }

    public void addSimpleRulesAction(int row, int col, String actionName, String[] parameterNames, String conditionStatement, IOpenClass[] columnTypes) {
        simpleRulesActionMap.put(CellKey.CellKeyFactory.getCellKey(col, row), new HeaderMetaInfo(actionName, parameterNames, conditionStatement, columnTypes));
    }

    public void addSimpleRulesReturn(int row, int col, String description) {
        simpleRulesReturnDescriptions.put(CellKey.CellKeyFactory.getCellKey(col, row), description);
    }

    private void saveValueMetaInfo(FunctionalRow funcRow, IGridRegion region) {
        // Lookup tables are transformed to Rules tables so we can't predict real column and row of a cell.
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
                    List<NodeUsage> nodeUsages = OpenLCellExpressionsCompiler.getNodeUsages(
                            (CompositeMethod) storageValue,
                            stringValue.substring(startIndex),
                            startIndex
                    );
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

    private void saveDescriptionMetaInfo(FunctionalRow funcRow, IGridRegion region) {
        // Condition/Action code (expression)
        ICell codeCell = funcRow.getCodeTable().getCell(0, 0);
        int row = codeCell.getAbsoluteRow();
        int col = codeCell.getAbsoluteColumn();
        if (IGridRegion.Tool.contains(region, col, row)) {
            List<CellMetaInfo> metaInfoList = OpenLCellExpressionsCompiler.getMetaInfo(
                    funcRow.getSourceCodeModule(),
                    funcRow.getMethod()
            );
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

                    return new CellMetaInfo(
                            JavaOpenClass.STRING,
                            false,
                            Collections.singletonList(nodeUsage));
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
        if (previous != null) {
            if (previous.getUsedNodes() != null) {
                metaInfo.setUsedNodes(previous.getUsedNodes());
            }
        }
        setPreparedMetaInfo(row, col, metaInfo);
    }
    
    private static class HeaderMetaInfo {
        String headerName;
        String[] parameterNames;
        String statement;
        IOpenClass[] columnTypes;

        public HeaderMetaInfo(String headerName,
                String[] parameterNames,
                String conditionStatement,
                IOpenClass[] columnTypes) {
            super();
            this.headerName = headerName;
            this.parameterNames = parameterNames;
            this.statement = conditionStatement;
            this.columnTypes = columnTypes;
        }

        public String getConditionName() {
            return headerName;
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
