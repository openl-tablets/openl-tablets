/*
 * Created on Oct 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.element.Action;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

/**
 * @author snshor
 * 
 */
public class DecisionTableLoader {
    
    /**
     * protected modified is for tests access.
     */
    protected static final String EMPTY_BODY = "Decision table must contain body section.";

    private int columnsNumber;

    private RuleRow ruleRow;

    private List<ICondition> conditions = new ArrayList<ICondition>();
    private List<IAction> actions = new ArrayList<IAction>();

    private void addAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, false));
    }

    private void addCondition(String name, int row, ILogicalTable table) {
        conditions.add(new Condition(name, row, table));
    }

    private void addReturnAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, true));
    }

    private void addRule(int row, ILogicalTable table) throws SyntaxNodeException {

        if (ruleRow != null) {

            throw SyntaxNodeExceptionUtils.createError("Only one rule row/column allowed",
                new GridCellSourceCodeModule(table.getLogicalRow(row).getGridTable(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0));
        }

        ruleRow = new RuleRow(row, table);
    }

    public DecisionTable loadAndBind(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContext) throws Exception {

        loadTableStructure(tableSyntaxNode, decisionTable);

        ICondition[] conditionsArray = conditions.toArray(new ICondition[conditions.size()]);
        IAction[] actionsArray = actions.toArray(new IAction[actions.size()]);

        decisionTable.bindTable(conditionsArray, actionsArray, ruleRow, openl, module, bindingContext, columnsNumber);

        return decisionTable;
    }

    private void loadTableStructure(TableSyntaxNode tableSyntaxNode, DecisionTable decisionTable) throws SyntaxNodeException {

        decisionTable.setTableSyntaxNode(tableSyntaxNode);
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        
        if (tableBody == null) {
            throw new SyntaxNodeException(EMPTY_BODY, null, tableSyntaxNode);
        }
        ILogicalTable transposed = tableBody.transpose();
        ILogicalTable toParse = tableBody;

        // check if table is a lookup table
        if (isLookupDecisionTable(tableBody)) {

            try {
                IGridTable convertedTable = new DecisionTableLookupConvertor().convertTable(tableBody);
//               System.out.println(TablePrinter.printGridTable(convertedTable.getGridTable()));
                ILogicalTable offsetConvertedTable = LogicalTableHelper.logicalTable(convertedTable);
                toParse = offsetConvertedTable.transpose();
                tableBody = transposed;
            } catch (Exception e) {
                throw new SyntaxNodeException("Cannot convert table", e, tableSyntaxNode);
            }

        } else if (DecisionTableHelper.looksLikeTransposed(tableBody)) {
            toParse = tableBody = transposed;
        }

        if (toParse.getLogicalWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            throw new SyntaxNodeException("Invalid structure of decision table", null, tableSyntaxNode);
        }

        columnsNumber = toParse.getLogicalWidth() - IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;

        ILogicalTable businessView = tableBody.columns(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
        tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, businessView);

        for (int i = 0; i < toParse.getLogicalHeight(); i++) {
            loadRow(i, toParse);
        }
    }

    private boolean isLookupDecisionTable(ILogicalTable tableBody) {
        return DecisionTableHelper.hasHConditions(tableBody);
    }

    private void loadRow(int row, ILogicalTable table) throws SyntaxNodeException {

        String headerStr = table.getLogicalRow(row)
            .getGridTable()
            .getCell(IDecisionTableConstants.INFO_COLUMN_INDEX, 0)
            .getStringValue();

        if (headerStr == null) {
            return;
        }

        String header = headerStr.toUpperCase();

        if (DecisionTableHelper.isConditionHeader(header)) {
            addCondition(headerStr, row, table);
        } else if (DecisionTableHelper.isValidActionHeader(header)) {
            addAction(headerStr, row, table);
        } else if (DecisionTableHelper.isValidRuleHeader(header)) {
            addRule(row, table);
        } else if (DecisionTableHelper.isValidRetHeader(header)) {
            addReturnAction(headerStr, row, table);
        } else if (DecisionTableHelper.isValidCommentHeader(header)) {
            // do nothing
        } else {
            throw SyntaxNodeExceptionUtils.createError("Invalid Decision Table header:" + headerStr,
                new GridCellSourceCodeModule(table.getLogicalRow(row).getGridTable(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0));

        }
    }

}