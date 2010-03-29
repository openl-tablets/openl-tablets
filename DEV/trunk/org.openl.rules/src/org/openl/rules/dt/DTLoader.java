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
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

/**
 * @author snshor
 *
 */
public class DTLoader {

    private RuleRow ruleRow;

    private List<DTAction> actions = new ArrayList<DTAction>();

    private int columns;

    private List<DTCondition> conditions = new ArrayList<DTCondition>();

    private void addAction(String name, int row, ILogicalTable table) {
        actions.add(new DTAction(name, row, table, false));
    }

    private void addCondition(String name, int row, ILogicalTable table) {
        conditions.add(new DTCondition(name, row, table));
    }

    private void addReturnAction(String name, int row, ILogicalTable table) {
        actions.add(new DTAction(name, row, table, true));
    }

    private void addRule(int row, ILogicalTable table) {
        if (ruleRow != null) {
            throw new RuntimeException("Only one rule row/column allowed");
        }
        ruleRow = new RuleRow(row, table);
    }

    private int countConditionsAndActions(ILogicalTable t) {
        int w = t.getLogicalWidth();
        int cnt = 0;
        for (int i = 0; i < w; i++) {
            String s = t.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (s != null) {
            	s = s.toUpperCase();
                cnt += isValidConditionHeader(s) || isActionHeader(s) ? 1 : 0;
            }
        }

        return cnt;
    }

    private boolean hasHConditions(ILogicalTable t) {
        int w = t.getLogicalWidth();
        for (int i = 0; i < w; i++) {
            String s = t.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (s != null) {
            	s = s.toUpperCase();
            	if (DTLookupConvertor.isValidHConditionHeader(s))
                return true;
            }
        }

        return false;
    }
    
    /**
     * 
     * @param s - not null string converted to uppercase before testing
     * the same is true for all isValid..() functions in this class
     * @return
     */
    public static boolean isValidConditionHeader(String s) {
    	return s.length() >=2 && s.charAt(0) == 'C' && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidActionHeader(String s) {
    	return s.length() >=2 && s.charAt(0) == 'A' && Character.isDigit(s.charAt(1));
    }
    
    public static boolean isValidRetHeader(String s) {
    	return s.length() >=3 && s.startsWith(IDecisionTableConstants.RETURN) && (s.length() ==  3 || Character
    	        .isDigit(s.charAt(3)));
    }

    public static boolean isValidRuleHeader(String s) {
    	return s.equals(IDecisionTableConstants.RULE);
    }
    
    public static boolean isValidCommentHeader(String s) {
    	return s.startsWith("//");
    }
    
    public static boolean isActionHeader(String s) {
    	return isValidActionHeader(s) || isValidRetHeader(s);
    }
    
    public static boolean isConditionHeader(String s) {
    	return isValidConditionHeader(s) || DTLookupConvertor.isValidHConditionHeader(s);
    }
    
    public DecisionTable load(TableSyntaxNode tsn, DecisionTable dt, OpenL openl, ModuleOpenClass module,
            IBindingContextDelegator cxtd) throws Exception {

        dt.setTableSyntaxNode(tsn);
        ILogicalTable tableBody = tsn.getTableBody();
        
        ILogicalTable transposed = tableBody.transpose();
        
        ILogicalTable toParse = tableBody;
        
        if (hasHConditions(tableBody)) {	
        	toParse = LogicalTable.logicalTable( new DTLookupConvertor().convertTable(tableBody).transpose());
        	tableBody = transposed;
        }	
        else if (looksLikeTransposed(tableBody)) {
            toParse = tableBody = transposed;
        }

        if (toParse.getLogicalWidth() < IDecisionTableConstants.DATA_COLUMN) {
            throw new Exception("Invalid structure of decision table");
        }

        columns = toParse.getLogicalWidth() - IDecisionTableConstants.DATA_COLUMN;

        ILogicalTable businessView = tableBody.columns(IDecisionTableConstants.DATA_COLUMN - 1);
        tsn.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, businessView);

        for (int i = 0; i < toParse.getLogicalHeight(); i++) {
            loadRow(i, toParse);
        }

        // DecisionTableStructure dts = new DecisionTableStructure();
        // dts.setCondition(makeTemplates(conditions));
        // dts.setAction(makeTemplates(actions));

        dt.bindTable(conditions.toArray(new IDTCondition[0]), actions
                .toArray(new IDTAction[0]), ruleRow,
        // null,
                openl, module, cxtd, columns);

        return dt;
    }

    private void loadRow(int row, ILogicalTable table) throws SyntaxNodeException {

        String headerStr = table.getLogicalRow(row).getGridTable().getCell(IDecisionTableConstants.INFO_COLUMN, 0)
            .getStringValue();
        if (headerStr == null) {
            return;
        }

        String header = headerStr.toUpperCase();
        if (isConditionHeader(header)) {
            addCondition(headerStr, row, table);
        } else if (isValidActionHeader(header)) {
            addAction(headerStr, row, table);
        } else if (isValidRuleHeader(header)) {
            addRule(row, table);
        } else if (isValidRetHeader(header)) {
            addReturnAction(headerStr, row, table);
        }
        else if (isValidCommentHeader(header))
            ;

        else {
             throw SyntaxNodeExceptionUtils.createError("Invalid Decision Table header:" + headerStr, new GridCellSourceCodeModule(table.getLogicalRow(row).
                     getGridTable(), IDecisionTableConstants.INFO_COLUMN, 0));
            // ignore for now
        }
        // return row + empty;

    }

    private boolean looksLikeTransposed(ILogicalTable table) {
        if (table.getLogicalWidth() <= IDecisionTableConstants.DATA_COLUMN) {
            return true;
        }

        if (table.getLogicalHeight() <= IDecisionTableConstants.DATA_COLUMN) {
            return false;
        }

        int cnt1 = countConditionsAndActions(table);
        int cnt2 = countConditionsAndActions(table.transpose());

        if (cnt1 != cnt2) {
            return cnt1 > cnt2;
        }
        return table.getLogicalWidth() <= IDecisionTableConstants.DATA_COLUMN;
    }

}