package org.openl.rules.lang.xls.types.meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.openl.rules.table.*;
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
    private final Map<CellKey, Integer> simpleRulesConditionMap = new HashMap<>();

    /**
     * Map for compound return column descriptions in SimpleRules header
     */
    private final Map<CellKey, String> simpleRulesReturnDescriptions = new HashMap<>();

    public DecisionTableMetaInfoReader(DecisionTableBoundNode boundNode) {
        super(boundNode);
        decisionTable = null;
    }

    public DecisionTableMetaInfoReader(DecisionTableBoundNode boundNode, DecisionTable decisionTable) {
        super(boundNode);
        this.decisionTable = decisionTable;
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        DecisionTable decisionTable = getDecisionTable();

        CellMetaInfo compoundReturnMeta = searchCompoundReturnColumn(row, col);
        if (compoundReturnMeta != NOT_FOUND) {
            return compoundReturnMeta;
        }

        // Condition description
        IBaseCondition[] conditionRows = decisionTable.getConditionRows();
        for (IBaseCondition conditionRow : conditionRows) {
            FunctionalRow funcRow = (FunctionalRow) conditionRow;

            CellMetaInfo metaInfo = searchSimpleRulesMetaInfo(decisionTable, funcRow, row, col);
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }

            metaInfo = searchDescriptionMetaInfo(funcRow, row, col);
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }
        }

        // Action description
        for (IBaseAction action : decisionTable.getActionRows()) {
            CellMetaInfo metaInfo = searchDescriptionMetaInfo((FunctionalRow) action, row, col);
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }
        }

        // Condition values
        for (IBaseCondition condition : decisionTable.getConditionRows()) {
            FunctionalRow funcRow = (FunctionalRow) condition;
            CellMetaInfo metaInfo = searchValueMetaInfo(funcRow, row, col);
            if (metaInfo != NOT_FOUND) {
                // Cell is found and it can be null
                return metaInfo;
            }
        }

        // Action values
        for (IBaseAction action : decisionTable.getActionRows()) {
            FunctionalRow funcRow = (FunctionalRow) action;
            CellMetaInfo metaInfo = searchValueMetaInfo(funcRow, row, col);
            if (metaInfo != NOT_FOUND) {
                // Cell is found and it can be null
                return metaInfo;
            }
        }

        return null;
    }

    private CellMetaInfo searchSimpleRulesMetaInfo(DecisionTable decisionTable, FunctionalRow funcRow, int row, int col) {
        Integer paramIndex = simpleRulesConditionMap.get(CellKey.CellKeyFactory.getCellKey(col, row));
        if (paramIndex != null) {
            // SimpleRules or SimpleLookup
            String text = String.format("Condition for %s: %s",
                    decisionTable.getSignature().getParameterName(paramIndex),
                    decisionTable.getSignature().getParameterType(paramIndex).getDisplayName(0));
            IGrid grid = funcRow.getDecisionTable()
                    .getSource()
                    .getGrid();
            if (grid instanceof CompositeGrid) {
                grid = ((CompositeGrid) grid).getGridTables()[1].getGrid(); // Get original grid
            }
            String cellValue = grid.getCell(col, row).getStringValue();
            if (StringUtils.isBlank(cellValue)) {
                return null;
            }
            SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
                    cellValue.length() - 1,
                    text,
                    null,
                    NodeType.OTHER);
            return new CellMetaInfo(
                    JavaOpenClass.STRING,
                    false,
                    Collections.singletonList(simpleNodeUsage));
        }

        return NOT_FOUND;
    }

    private CellMetaInfo searchCompoundReturnColumn(int row, int col) {
        String description = simpleRulesReturnDescriptions.get(CellKey.CellKeyFactory.getCellKey(col, row));
        if (description != null) {
            ICell cell = getTableSyntaxNode().getGridTable().getGrid().getCell(col, row);
            String stringValue = cell.getStringValue();

            SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
                    stringValue.length() - 1,
                    description,
                    null,
                    NodeType.OTHER);
            return new CellMetaInfo(
                    JavaOpenClass.STRING,
                    false,
                    Collections.singletonList(simpleNodeUsage));
        }

        return NOT_FOUND;
    }

    public void addSimpleRulesCondition(int row, int col, int paramIndex) {
        simpleRulesConditionMap.put(CellKey.CellKeyFactory.getCellKey(col, row), paramIndex);
    }

    public void addSimpleRulesReturn(int row, int col, String description) {
        simpleRulesReturnDescriptions.put(CellKey.CellKeyFactory.getCellKey(col, row), description);
    }

    private CellMetaInfo searchValueMetaInfo(FunctionalRow funcRow, int row, int col) {
        // Lookup tables are transformed to Rules tables so we can't predict real column and row of a cell.
        // In current implementation we run through all of them and if it's current row and cell.
        for (int c = 0; c < funcRow.nValues(); c++) {
            for (int i = 0; i < funcRow.getNumberOfParams(); i++) {
                ILogicalTable valueCell = funcRow.getValueCell(c);
                ICell cell = valueCell.getCell(0, 0);
                if (isNeededCell(cell, row, col)) {
                    Object storageValue = funcRow.getStorageValue(i, c);
                    if (storageValue instanceof CompositeMethod) {
                        String stringValue = cell.getStringValue();
                        List<NodeUsage> nodeUsages = OpenLCellExpressionsCompiler.getNodeUsages(
                                (CompositeMethod) storageValue,
                                stringValue,
                                stringValue.indexOf('=') + 1
                        );
                        return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
                    }

                    IOpenClass type = funcRow.getParams()[i].getType();
                    boolean multiValue = false;
                    if (type.isArray()) {
                        multiValue = true;
                        type = type.getAggregateInfo().getComponentType(type);
                    }
                    return new CellMetaInfo(type, multiValue);
                }
            }
        }

        return NOT_FOUND;
    }

    private CellMetaInfo searchDescriptionMetaInfo(FunctionalRow funcRow, int row, int col) {
        // Condition/Action code (expression)
        ICell codeCell = funcRow.getCodeTable().getCell(0, 0);
        if (isNeededCell(codeCell, row, col)) {
            List<CellMetaInfo> metaInfoList = OpenLCellExpressionsCompiler.getMetaInfo(
                    funcRow.getSourceCodeModule(),
                    funcRow.getMethod()
            );
            // Decision table always contains 1 meta info
            return metaInfoList.get(0);
        }

        // Condition/Action type definition
        ILogicalTable paramsTable = funcRow.getParamsTable();
        if (funcRow.getNumberOfParams() > 0) {
            IParameterDeclaration param = funcRow.getParams()[0];
            ICell paramCell = paramsTable.getCell(0, 0);
            if (isNeededCell(paramCell, row, col)) {
                return getMetaInfo(paramsTable, param.getType());
            }
        }

        return NOT_FOUND;
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

}
