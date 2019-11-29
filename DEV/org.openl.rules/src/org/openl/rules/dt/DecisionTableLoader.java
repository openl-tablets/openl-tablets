package org.openl.rules.dt;

import org.apache.commons.lang3.StringUtils;
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
import org.openl.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.openl.rules.dt.DecisionTableHelper.isSimple;
import static org.openl.rules.dt.DecisionTableHelper.isSmart;

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
    private List<IBaseCondition> conditions = new ArrayList<>();
    private List<IBaseAction> actions = new ArrayList<>();
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
        if (DecisionTableHelper.isValidHConditionHeader(name.toUpperCase())) {
            return info.getScale().getHScale();
        }
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
            throw SyntaxNodeExceptionUtils.createError("Only one rule row/column allowed.",
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

        // preprocess decision tables (without conditions and return headers)
        // add virtual headers to the table body.
        //
        if (DecisionTableHelper.isSmartDecisionTable(tableSyntaxNode) || DecisionTableHelper
            .isSimpleDecisionTable(tableSyntaxNode) || DecisionTableHelper
                .isSimpleLookupTable(tableSyntaxNode) || DecisionTableHelper.isSmartLookupTable(tableSyntaxNode)) {
            try {
                tableBody = DecisionTableHelper
                    .preprocessDecisionTableWithoutHeaders(tableSyntaxNode, decisionTable, tableBody, bindingContext);
            } catch (OpenLCompilationException e) {
                throw SyntaxNodeExceptionUtils
                    .createError("Cannot create a header for a Simple Rules or Lookup Table.", e, tableSyntaxNode);
            }
        }
        ILogicalTable toParse = tableBody;

        // process lookup decision table.
        //

        int nHConditions = DecisionTableHelper.countHConditionsByHeaders(tableBody);
        int nVConditions = DecisionTableHelper.countVConditionsByHeaders(tableBody);
        if (nHConditions > 0) {
            try {
                DecisionTableLookupConvertor dtlc = new DecisionTableLookupConvertor();

                IGridTable convertedTable = dtlc.convertTable(tableBody);
                ILogicalTable offsetConvertedTable = LogicalTableHelper.logicalTable(convertedTable);
                toParse = offsetConvertedTable.transpose();
                info = new DTInfo(nHConditions, nVConditions, dtlc.getScale());
            } catch (OpenLCompilationException e) {
                throw SyntaxNodeExceptionUtils.createError(e, tableSyntaxNode);
            } catch (Exception e) {
                throw SyntaxNodeExceptionUtils.createError("Cannot convert table.", e, tableSyntaxNode);
            }

        } else if (DecisionTableHelper.looksLikeVertical(tableBody)) {
            // parsing is based on horizontal representation of decision table.
            //
            toParse = tableBody.transpose();
        }

        if (needToUnmergeFirstRow(toParse)) {
            toParse = unmergeFirstRow(toParse);
        }

        if (info == null) {
            info = new DTInfo(nHConditions, nVConditions);
        }
        decisionTable.setDtInfo(info);

        if (toParse.getWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            throw SyntaxNodeExceptionUtils.createError("Invalid structure of decision table.", tableSyntaxNode);
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
     * @param tableSyntaxNode table syntax node
     */
    private void putTableForBusinessView(TableSyntaxNode tableSyntaxNode) {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();

        if (DecisionTableHelper.isSmartDecisionTable(tableSyntaxNode) || DecisionTableHelper
            .isSimpleDecisionTable(tableSyntaxNode) || DecisionTableHelper
                .isSimpleLookupTable(tableSyntaxNode) || DecisionTableHelper.isSmartLookupTable(tableSyntaxNode)) {
            // if DT is simple, its body does not contain conditions and return headers.
            // so put the body as it is.
            tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);
        } else {
            // need to get the subtable without conditions and return headers.
            ILogicalTable businessView;
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

    private String getHeaderStr(int row, ILogicalTable table) {
        String headerStr = table.getRow(row)
            .getSource()
            .getCell(IDecisionTableConstants.INFO_COLUMN_INDEX, 0)
            .getStringValue();

        if (headerStr == null) {
            return StringUtils.EMPTY;
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
                    String.format("Invalid Decision Table header '%s'. Headers '%s' and '%s' cannot be used together.",
                        header,
                        firstUsedReturnActionHeader,
                        header),
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
                    String.format("Invalid Decision Table header '%s'. Headers '%s' and '%s' cannot be used together.",
                        header,
                        firstUsedReturnActionHeader,
                        header),
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
                if (isSmart(decisionTable.getSyntaxNode()) || isSimple(decisionTable.getSyntaxNode())) {
                    boolean isMap = decisionTable.getSyntaxNode().getHeader().getCollectParameters().length > 0;
                    final String errorMsg = String.format(
                        "Decision table return type '%s' is incompatible with keyword 'Collect' in the table header, expected %s.",
                        decisionTable.getType().getName(),
                        isMap ? "a map" : "an array or a collection");
                    throw SyntaxNodeExceptionUtils.createError(errorMsg, decisionTable.getSyntaxNode());
                } else {
                    throw SyntaxNodeExceptionUtils.createError(String.format(
                        "Decision table return type '%s' is incompatible with column header '%s'.",
                        decisionTable.getType().getName(),
                        header),
                        new GridCellSourceCodeModule(table.getRow(row).getSource(),
                            IDecisionTableConstants.INFO_COLUMN_INDEX,
                            0,
                            bindingContext));
                }
            }
        } else if (!ParserUtils.isBlankOrCommented(header)) {
            throw SyntaxNodeExceptionUtils.createError(String.format("Invalid Decision Table header '%s'.", header),
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
        if (ClassUtils.isAssignable(type.getInstanceClass(), Collection.class)) {
            return true;
        }
        return ClassUtils.isAssignable(type.getInstanceClass(), Map.class);

    }

}