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
import org.openl.rules.table.IGridTable;
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

    private void addAction(String name, int row, IGridTable table) {
        actions.add(new Action(name, row, table, false));
    }

    private void addCondition(String name, int row, IGridTable table) {
        conditions.add(new Condition(name, row, table));
    }

    private void addReturnAction(String name, int row, IGridTable table) {
        actions.add(new Action(name, row, table, true));
    }

    private void addRule(int row, IGridTable table) throws SyntaxNodeException {

        if (ruleRow != null) {

            throw SyntaxNodeExceptionUtils.createError("Only one rule row/column allowed",
                new GridCellSourceCodeModule(table.getRow(row).getGridTable(),
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
        IGridTable tableBody = tableSyntaxNode.getTableBody();
        
        if (tableBody == null) {
            throw new SyntaxNodeException(EMPTY_BODY, null, tableSyntaxNode);
        }
        IGridTable transposed = tableBody.transpose();
        IGridTable toParse = tableBody;

        // check if table is a lookup table
        if (isLookupDecisionTable(tableBody)) {

            try {
                IGridTable convertedTable = new DecisionTableLookupConvertor().convertTable(tableBody);
//               System.out.println(TablePrinter.printGridTable(convertedTable.getOriginalGridTable()));
                IGridTable offsetConvertedTable = LogicalTableHelper.logicalTable(convertedTable);
                toParse = offsetConvertedTable.transpose();
                tableBody = transposed;
            } catch (Exception e) {
                throw new SyntaxNodeException("Cannot convert table", e, tableSyntaxNode);
            }

        } else if (DecisionTableHelper.looksLikeTransposed(tableBody)) {
            toParse = tableBody = transposed;
        }

        if (toParse.getGridWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            throw new SyntaxNodeException("Invalid structure of decision table", null, tableSyntaxNode);
        }

        columnsNumber = toParse.getGridWidth() - IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;

        IGridTable businessView = tableBody.columns(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
        tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, businessView);

        for (int i = 0; i < toParse.getGridHeight(); i++) {
            loadRow(i, toParse);
        }
    }

    private boolean isLookupDecisionTable(IGridTable tableBody) {
        return DecisionTableHelper.hasHConditions(tableBody);
    }

    private void loadRow(int row, IGridTable table) throws SyntaxNodeException {

        String headerStr = table.getRow(row)
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
                new GridCellSourceCodeModule(table.getRow(row).getGridTable(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0));

        }
    }

}