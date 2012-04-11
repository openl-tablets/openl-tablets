/*
 * Created on Oct 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLCompilationException;
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

    private void addRule(int row, ILogicalTable table, IBindingContext bindingContext) throws SyntaxNodeException {

        if (ruleRow != null) {

            throw SyntaxNodeExceptionUtils.createError("Only one rule row/column allowed",
                new GridCellSourceCodeModule(table.getRow(row).getSource(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0, bindingContext));
        }

        ruleRow = new RuleRow(row, table);
    }

    public DecisionTable loadAndBind(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContext) throws Exception {

        loadTableStructure(tableSyntaxNode, decisionTable, bindingContext);

        ICondition[] conditionsArray = conditions.toArray(new ICondition[conditions.size()]);
        IAction[] actionsArray = actions.toArray(new IAction[actions.size()]);

        decisionTable.bindTable(conditionsArray, actionsArray, ruleRow, openl, module, bindingContext, columnsNumber);

        return decisionTable;
    }

    private void loadTableStructure(TableSyntaxNode tableSyntaxNode, DecisionTable decisionTable,
            IBindingContext bindingContext) throws SyntaxNodeException {

        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        
        if (tableBody == null) {
            throw new SyntaxNodeException(EMPTY_BODY, null, tableSyntaxNode);
        }
        
        // preprocess simple decision tables (without conditions and return headers)
        // add virtual headers to the table body.
        //
        try {
            tableBody = preprocessSimpleDecisionTable(tableSyntaxNode, decisionTable, tableBody);
        } catch (OpenLCompilationException e) {
            throw new SyntaxNodeException("Cannot create a header for a Simple Rules or Lookup Table", e, tableSyntaxNode);
        }

        ILogicalTable toParse = tableBody;
        
        // process lookup decision table.
        //
        if (hasHConditions(tableBody)) {
            try {
                IGridTable convertedTable = new DecisionTableLookupConvertor().convertTable(tableBody);
                ILogicalTable offsetConvertedTable = LogicalTableHelper.logicalTable(convertedTable);
                toParse = offsetConvertedTable.transpose();
            } catch (Exception e) {
                throw new SyntaxNodeException("Cannot convert table", e, tableSyntaxNode);
            }

        } else if (DecisionTableHelper.looksLikeVertical(tableBody)) {
            // parsing is based on horizontal representation of decision table.
            //
            toParse = tableBody.transpose();
        }

        if (toParse.getWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            throw new SyntaxNodeException("Invalid structure of decision table", null, tableSyntaxNode);
        }

        columnsNumber = toParse.getWidth() - IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
        
        // NOTE! this method call depends on upper stacks calls, don`t move it upper.
        //
        putTableForBusinessView(tableSyntaxNode);
        
        for (int i = 0; i < toParse.getHeight(); i++) {
            loadRow(i, toParse, bindingContext);
        }
    }
    
    /**
     * Put subtable, that will be displayed at the business view.<br> 
     * It must be without method header, properties section, conditions and return headers. 
     * 
     * @param tableSyntaxNode
     */
    private void putTableForBusinessView(TableSyntaxNode tableSyntaxNode) {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        
        if (DecisionTableHelper.isSimpleDecisionTable(tableSyntaxNode) || DecisionTableHelper.isSimpleLookupTable(tableSyntaxNode)) {
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
    
    /**
     * Adds conditions and return headers to simple Decision table body.<br>
     * Supports simple Desicion Table and simple lookup Desicion Table.
     * 
     * @param tableSyntaxNode 
     * @param decisionTable method description for simple Desicion Table.
     * @param tableBody original simple Decision Table body
     * @return table body with added conditions and return headers.
     */
    private ILogicalTable preprocessSimpleDecisionTable(TableSyntaxNode tableSyntaxNode, DecisionTable decisionTable,
            ILogicalTable tableBody) throws OpenLCompilationException {
        
        if (DecisionTableHelper.isSimpleDecisionTable(tableSyntaxNode)) {
            tableBody = DecisionTableHelper.preprocessSimpleDecisionTable(decisionTable, tableBody, 0);
        } else if (DecisionTableHelper.isSimpleLookupTable(tableSyntaxNode)) {
            tableBody = DecisionTableHelper.preprocessSimpleDecisionTable(decisionTable, tableBody, tableBody.getSource()
                    .getCell(0, 0).getHeight());
        }
        return tableBody;
    }

    private boolean hasHConditions(ILogicalTable tableBody) {
        return DecisionTableHelper.hasHConditions(tableBody);
    }

    private void loadRow(int row, ILogicalTable table, IBindingContext bindingContext) throws SyntaxNodeException {

        String headerStr = table.getRow(row)
            .getSource()
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
            addRule(row, table, bindingContext);
        } else if (DecisionTableHelper.isValidRetHeader(header)) {
            addReturnAction(headerStr, row, table);
        } else if (DecisionTableHelper.isValidCommentHeader(header)) {
            // do nothing
        } else {
            throw SyntaxNodeExceptionUtils.createError("Invalid Decision Table header:" + headerStr,
                new GridCellSourceCodeModule(table.getRow(row).getSource(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0, bindingContext));

        }
    }

}