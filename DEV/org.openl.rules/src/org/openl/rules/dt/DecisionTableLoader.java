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
import org.openl.rules.dt.DTScale.RowScale;
import org.openl.rules.dt.element.Action;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.ParserUtils;
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
    
   DTInfo info;
    

    private List<IBaseCondition> conditions = new ArrayList<IBaseCondition>();
    private List<IBaseAction> actions = new ArrayList<IBaseAction>();

    private void addAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, false, DTScale.getStandardScale()));
    }

    private void addCondition(String name, int row, ILogicalTable table) {
        conditions.add(new Condition(name, row, table, getConditionScale(name)));
    }

    private RowScale getConditionScale(String name) {
    	if (DecisionTableHelper.isValidHConditionHeader(name.toUpperCase()) )
    		return info.getScale().getHScale();
		return info.getScale().getVScale();
	}

	private void addReturnAction(String name, int row, ILogicalTable table) {
        actions.add(new Action(name, row, table, true, DTScale.getStandardScale()));
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
            throw SyntaxNodeExceptionUtils.createError(EMPTY_BODY, tableSyntaxNode);
        }

        // preprocess simple decision tables (without conditions and return headers)
        // add virtual headers to the table body.
        //
        try {
            tableBody = preprocessSimpleDecisionTable(tableSyntaxNode, decisionTable, tableBody);
        } catch (OpenLCompilationException e) {
            throw SyntaxNodeExceptionUtils.createError("Cannot create a header for a Simple Rules or Lookup Table", e, tableSyntaxNode);
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
                throw SyntaxNodeExceptionUtils.createError("Cannot convert table", e, tableSyntaxNode);
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
            loadRow(i, toParse, bindingContext);
        }
    }
    
    private ILogicalTable unmergeFirstRow(ILogicalTable toParse) {
    	ILogicalTable unmerged  = LogicalTableHelper.unmergeColumns(toParse, IDecisionTableConstants.SERVICE_COLUMNS_NUMBER, toParse.getWidth());
    	 
		return unmerged;
	}

	private boolean needToUnmergeFirstRow(ILogicalTable toParse) {
		String header = getHeaderStr(0, toParse);
		
		return DecisionTableHelper.isConditionHeader(header)  && !DecisionTableHelper.isValidMergedConditionHeader(header);
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

    private int countHConditions(ILogicalTable tableBody) {
        return DecisionTableHelper.countHConditions(tableBody);
    }

    private int countVConditions(ILogicalTable tableBody) {
        return DecisionTableHelper.countVConditions(tableBody);
    }
    
    
    private String getHeaderStr(int row, ILogicalTable table)
    {
        String headerStr = table.getRow(row)
                .getSource()
                .getCell(IDecisionTableConstants.INFO_COLUMN_INDEX, 0)
                .getStringValue();

            if (headerStr == null) {
                return "";
            }

            return headerStr.toUpperCase();
    }
    
    
    private void loadRow(int row, ILogicalTable table, IBindingContext bindingContext) throws SyntaxNodeException {

        String header = getHeaderStr(row, table);

        if (DecisionTableHelper.isConditionHeader(header)) {
            addCondition(header, row, table);
        } else if (DecisionTableHelper.isValidActionHeader(header)) {
            addAction(header, row, table);
        } else if (DecisionTableHelper.isValidRuleHeader(header)) {
            addRule(row, table, bindingContext);
        } else if (DecisionTableHelper.isValidRetHeader(header)) {
            addReturnAction(header, row, table);
        } else if (ParserUtils.isBlankOrCommented(header)) {
            // do nothing
        } else {
            throw SyntaxNodeExceptionUtils.createError("Invalid Decision Table header:" + header,
                new GridCellSourceCodeModule(table.getRow(row).getSource(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0, bindingContext));

        }
    }

}