package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.dt.DTScale.RowScale;
import org.openl.rules.dt.element.Action;
import org.openl.rules.dt.element.ActionType;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.ParserUtils;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class DecisionTableLoader {

    /**
     * protected modified is for tests access.
     */
    static final String EMPTY_BODY = "Decision table must contain body section.";

    private int columnsNumber;

    private RuleRow ruleRow;

    private DTInfo info;

    private List<IBaseCondition> conditions = new ArrayList<IBaseCondition>();
    private List<IBaseAction> actions = new ArrayList<IBaseAction>();
    private boolean hasReturnAction = false;
    private boolean hasCollectReturnAction = false;
    private boolean hasCollectReturnKeyAction = false;
    private String firstUsedReturnActionHeader = null;

    private void addAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, ActionType.ACTION, DTScale.getStandardScale()));
    }

    private void addCondition(String name, int row, ILogicalTable table) {
        conditions.add(new Condition(name, row, table, getConditionScale(name)));
    }

    private RowScale getConditionScale(String name) {
        if (DecisionTableHelper.isValidHConditionHeader(name.toUpperCase()))
            return info.getScale().getHScale();
        return info.getScale().getVScale();
    }

    private void addReturnAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, ActionType.RETURN, DTScale.getStandardScale()));
    }

    private void addCollectReturnKeyAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, ActionType.COLLECT_RETURN_KEY, DTScale.getStandardScale()));
    }

    private void addCollectReturnAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, ActionType.COLLECT_RETURN, DTScale.getStandardScale()));
    }

    private void addRule(int row, ILogicalTable table, IBindingContext bindingContext) throws SyntaxNodeException {

        if (ruleRow != null) {

            throw SyntaxNodeExceptionUtils.createError("Only one rule row/column allowed",
                new GridCellSourceCodeModule(table.getRow(row).getSource(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0,
                    bindingContext));
        }

        ruleRow = new RuleRow(row, table);
    }

    public void loadAndBind(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContext bindingContext) throws Exception {

        loadTableStructure(tableSyntaxNode, decisionTable, bindingContext);

        IBaseCondition[] conditionsArray = conditions.toArray(IBaseCondition.EMPTY);
        IBaseAction[] actionsArray = actions.toArray(IBaseAction.EMPTY);

        decisionTable.bindTable(conditionsArray, actionsArray, ruleRow, openl, module, bindingContext, columnsNumber);
    }

    private void loadTableStructure(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            IBindingContext bindingContext) throws SyntaxNodeException {

        ILogicalTable tableBody = tableSyntaxNode.getTableBody();

        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError(EMPTY_BODY, tableSyntaxNode);
        }

        // preprocess simple decision tables (without conditions and return headers)
        // add virtual headers to the table body.
        //
        try {
            tableBody = preprocessSimpleDecisionTable(tableSyntaxNode, decisionTable, tableBody, bindingContext);
        } catch (OpenLCompilationException e) {
            throw SyntaxNodeExceptionUtils
                .createError("Can't create a header for a Simple Rules or Lookup Table", e, tableSyntaxNode);
        }

        ILogicalTable toParse = tableBody;

        // process lookup decision table.
        //

        int nHConditions = countHConditions(tableBody);
        int nVConditions = countVConditions(tableBody);
        if (nHConditions > 0) {
            try {
                DecisionTableLookupConvertor dtlc = new DecisionTableLookupConvertor();

                IGridTable convertedTable = dtlc.convertTable(tableBody);
                ILogicalTable offsetConvertedTable = LogicalTableHelper.logicalTable(convertedTable);
                toParse = offsetConvertedTable.transpose();
                info = new DTInfo(nHConditions, nVConditions, dtlc.getScale());

            } catch (Exception e) {
                throw SyntaxNodeExceptionUtils.createError("Can't convert table", e, tableSyntaxNode);
            }

        } else if (DecisionTableHelper.looksLikeVertical(tableBody)) {
            // parsing is based on horizontal representation of decision table.
            //
            toParse = tableBody.transpose();
        }

        if (needToUnmergeFirstRow(toParse))
            toParse = unmergeFirstRow(toParse);

        if (info == null)
            info = new DTInfo(nHConditions, nVConditions);
        decisionTable.setDtInfo(info);

        if (toParse.getWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            throw SyntaxNodeExceptionUtils.createError("Invalid structure of decision table", tableSyntaxNode);
        }

        columnsNumber = toParse.getWidth() - IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;

        // NOTE! this method call depends on upper stacks calls, don`t move it upper.
        //
        putTableForBusinessView(tableSyntaxNode);

        for (int i = 0; i < toParse.getHeight(); i++) {
            loadRow(i, toParse, decisionTable, bindingContext);
        }

        validateMapReturnType(decisionTable, tableSyntaxNode);
    }

    private void validateMapReturnType(DecisionTable decisionTable,
            TableSyntaxNode tableSyntaxNode) throws SyntaxNodeException {
        if (Map.class.isAssignableFrom(decisionTable.getType().getInstanceClass())) {
            if (hasCollectReturnAction && !hasCollectReturnKeyAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Invalid Decision Table headers: At least one KEY header is required.",
                    tableSyntaxNode);
            }
            if (hasCollectReturnKeyAction && !hasCollectReturnAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Invalid Decision Table headers: At least one CRET header is required.",
                    tableSyntaxNode);
            }
        }
    }

    private ILogicalTable unmergeFirstRow(ILogicalTable toParse) {

        return LogicalTableHelper
            .unmergeColumns(toParse, IDecisionTableConstants.SERVICE_COLUMNS_NUMBER, toParse.getWidth());
    }

    private boolean needToUnmergeFirstRow(ILogicalTable toParse) {
        String header = getHeaderStr(0, toParse);

        return DecisionTableHelper
            .isConditionHeader(header) && !DecisionTableHelper.isValidMergedConditionHeader(header);
    }

    /**
     * Put subtable, that will be displayed at the business view.<br>
     * It must be without method header, properties section, conditions and return headers.
     * 
     * @param tableSyntaxNode
     */
    private void putTableForBusinessView(TableSyntaxNode tableSyntaxNode) {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();

        if (DecisionTableHelper.isSimpleDecisionTableOrSmartDecisionTable(tableSyntaxNode) || DecisionTableHelper
            .isSimpleLookupTable(tableSyntaxNode)) {
            // if DT is simple, its body doesn`t contain conditions and return headers.
            // so put the body as it is.
            tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);
        } else {
            // need to get the subtable without conditions and return headers.
            ILogicalTable businessView = null;
            if (DecisionTableHelper.looksLikeVertical(tableBody)) {
                // if table is vertical, remove service rows.
                businessView = tableBody.getRows(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
            } else {
                // table is horizontal, so remove service columns.
                businessView = tableBody.getColumns(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
            }

            tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, businessView);
        }

    }

    private int getNumberOfHConditions(ILogicalTable tableBody) {
        int h = tableBody.getSource().getWidth();
        int d = tableBody.getSource().getCell(0, 0).getHeight();
        int k = 0;
        int i = 0;
        while (i < d) {
            i = i + tableBody.getSource().getCell(h - 1, i).getHeight();
            k++;
        }
        return k;
    }

    /**
     * Adds conditions and return headers to simple Decision table body.<br>
     * Supports simple Decision Table and simple lookup Decision Table.
     * 
     * @param tableSyntaxNode
     * @param decisionTable method description for simple Decision Table.
     * @param tableBody original simple Decision Table body
     * @return table body with added conditions and return headers.
     */
    private ILogicalTable preprocessSimpleDecisionTable(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable tableBody,
            IBindingContext bindingContext) throws OpenLCompilationException {

        if (DecisionTableHelper.isSimpleDecisionTableOrSmartDecisionTable(tableSyntaxNode)) {
            tableBody = DecisionTableHelper.preprocessSimpleDecisionTable(tableSyntaxNode,
                decisionTable,
                tableBody,
                0,
                tableBody.getSource().getCell(0, 0).getHeight(),
                DecisionTableHelper.isSmartDecisionTable(tableSyntaxNode),
                DecisionTableHelper.isCollectDecisionTable(tableSyntaxNode),
                bindingContext);
        } else if (DecisionTableHelper.isSimpleLookupTable(tableSyntaxNode)) {
            tableBody = DecisionTableHelper.preprocessSimpleDecisionTable(tableSyntaxNode,
                decisionTable,
                tableBody,
                getNumberOfHConditions(tableBody),
                tableBody.getSource().getCell(0, 0).getHeight(),
                DecisionTableHelper.isSmartSimpleLookupTable(tableSyntaxNode),
                DecisionTableHelper.isCollectDecisionTable(tableSyntaxNode),
                bindingContext);
        }

        return tableBody;
    }

    private int countHConditions(ILogicalTable tableBody) {
        return DecisionTableHelper.countHConditions(tableBody);
    }

    private int countVConditions(ILogicalTable tableBody) {
        return DecisionTableHelper.countVConditions(tableBody);
    }

    private String getHeaderStr(int row, ILogicalTable table) {
        String headerStr = table.getRow(row)
            .getSource()
            .getCell(IDecisionTableConstants.INFO_COLUMN_INDEX, 0)
            .getStringValue();

        if (headerStr == null) {
            return "";
        }

        return headerStr.toUpperCase();
    }

    private void loadRow(int row,
            ILogicalTable table,
            DecisionTable decisionTable,
            IBindingContext bindingContext) throws SyntaxNodeException {

        String header = getHeaderStr(row, table);

        if (DecisionTableHelper.isConditionHeader(header)) {
            addCondition(header, row, table);
        } else if (DecisionTableHelper.isValidActionHeader(header)) {
            addAction(header, row, table);
        } else if (DecisionTableHelper.isValidRuleHeader(header)) {
            addRule(row, table, bindingContext);
        } else if (DecisionTableHelper.isValidKeyHeader(header)) {
            addCollectReturnKeyAction(header, row, table);
            hasCollectReturnKeyAction = true;
        } else if (DecisionTableHelper.isValidRetHeader(header)) {
            if (hasCollectReturnAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Invalid Decision Table header: " + header + ". Headers '" + firstUsedReturnActionHeader + "' and '" + header + "' can't be used together.",
                    new GridCellSourceCodeModule(table.getRow(row).getSource(),
                        IDecisionTableConstants.INFO_COLUMN_INDEX,
                        0,
                        bindingContext));
            }
            addReturnAction(header, row, table);
            saveFirstUsedReturnActionHeader(header);
            hasReturnAction = true;
        } else if (DecisionTableHelper.isValidCRetHeader(header)) {
            if (hasReturnAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Invalid Decision Table header: " + header + ". Headers '" + firstUsedReturnActionHeader + "' and '" + header + "' can't be used together.",
                    new GridCellSourceCodeModule(table.getRow(row).getSource(),
                        IDecisionTableConstants.INFO_COLUMN_INDEX,
                        0,
                        bindingContext));
            }
            hasCollectReturnAction = true;
            saveFirstUsedReturnActionHeader(header);
            if (validateCollectReturnType(decisionTable)) {
                addCollectReturnAction(header, row, table);
            } else {
                throw SyntaxNodeExceptionUtils.createError(
                    "Incompatible method return type with '" + header + "' header.",
                    new GridCellSourceCodeModule(table.getRow(row).getSource(),
                        IDecisionTableConstants.INFO_COLUMN_INDEX,
                        0,
                        bindingContext));
            }
        } else if (!ParserUtils.isBlankOrCommented(header)) {
            throw SyntaxNodeExceptionUtils.createError("Invalid Decision Table header: " + header,
                new GridCellSourceCodeModule(table.getRow(row).getSource(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0,
                    bindingContext));
        }
    }

    private void saveFirstUsedReturnActionHeader(String header) {
        if (firstUsedReturnActionHeader == null) {
            firstUsedReturnActionHeader = header;
        }
    }

    private boolean validateCollectReturnType(DecisionTable decisionTable) {
        IOpenClass type = decisionTable.getType();

        if (type.isArray()) {
            return true;
        }
        if (Collection.class.isAssignableFrom(type.getInstanceClass())) {
            return true;
        }
        return Map.class.isAssignableFrom(type.getInstanceClass());

    }

}