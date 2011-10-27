package org.openl.rules.calc;

import java.util.List;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.ArrayResultBuilder;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.calc.result.ScalarResultBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

/**
 * TODO: refactor
 * @author DLiauchuk
 *
 */
public class SpreadsheetBuilder {
    
    private SpreadsheetRowColumnExtractor rowColumnExtractor;
    private SpreadsheetCellsBuilder cellsBuilder;
    private SpreadsheetOpenClass spreadsheetType;
    
    public SpreadsheetBuilder(IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode) {
        this.rowColumnExtractor = new SpreadsheetRowColumnExtractor(tableSyntaxNode, bindingContext);
        this.cellsBuilder = new SpreadsheetCellsBuilder(rowColumnExtractor);
    }
    
    public void firstBuildPhase(IOpenMethodHeader spreadsheetHeader) {
        cellsBuilder.buildCells(getSpreadsheetOpenClass(spreadsheetHeader.getName()), spreadsheetHeader);
    }

    public void secondBuildPhase(Spreadsheet spreadsheet) {   
        spreadsheet.setRowNames(rowColumnExtractor.getRowNames());
        spreadsheet.setColumnNames(rowColumnExtractor.getColumnNames());

        cellsBuilder.extractCellValues(spreadsheet.getHeader());
        spreadsheet.setCells(cellsBuilder.getCells());            
        
        
        try {            
            spreadsheet.setResultBuilder(getResultBuilder(spreadsheet));
        } catch (SyntaxNodeException e) {
            rowColumnExtractor.getTableSyntaxNode().addError(e);
            BindHelper.processError(e, rowColumnExtractor.getBindingContext());            
        }
    }

    public Map<String, SpreadsheetHeaderDefinition> getVarDefinitions() {
        return rowColumnExtractor.getVarDefinitions();
    }

    public SpreadsheetOpenClass getSpreadsheetOpenClass(String spreadsheetName) {
        if (spreadsheetType == null) {
            spreadsheetType = new SpreadsheetOpenClass(null, spreadsheetName + "Type", rowColumnExtractor.getBindingContext().getOpenL());
        }
        
        return spreadsheetType;
    }
    
    public boolean isExistsReturnHeader() {
        return rowColumnExtractor.getReturnHeaderDefinition() != null;
    }
    
    private IResultBuilder getResultBuilder(Spreadsheet spreadsheet) throws SyntaxNodeException {
        IResultBuilder resultBuilder = null;
        
        SymbolicTypeDefinition symbolicTypeDefinition = null;
        
        if (rowColumnExtractor.getReturnHeaderDefinition() != null) {
            symbolicTypeDefinition = rowColumnExtractor.getReturnHeaderDefinition().findVarDef(SpreadsheetRowColumnExtractor.RETURN_NAME);
        }
        
        if (spreadsheet.getHeader().getType() == JavaOpenClass.VOID) {
            throw SyntaxNodeExceptionUtils.createError("Spreadsheet can not return 'void' type", rowColumnExtractor.getTableSyntaxNode());
        }
        
        if (spreadsheet.getHeader().getType() == JavaOpenClass.getOpenClass(SpreadsheetResult.class)) {
            if (isExistsReturnHeader()) {
                throw SyntaxNodeExceptionUtils.createError(
                        "If Spreadsheet return type is SpreadsheetResult, no return type is allowed",
                        symbolicTypeDefinition.getName());
            }

            resultBuilder = new DefaultResultBuilder();
        } else {
            // real return type
            //
            if (!isExistsReturnHeader()) {
                throw SyntaxNodeExceptionUtils.createError("There should be RETURN row or column for this return type",
                    rowColumnExtractor.getTableSyntaxNode());
            }            
            List<SpreadsheetCell> notEmptyReturnDefinitions = spreadsheet.listNonEmptyCells(rowColumnExtractor.getReturnHeaderDefinition());

            switch (notEmptyReturnDefinitions.size()) {
                case 0:
                    throw SyntaxNodeExceptionUtils.createError("There is no return expression cell",
                            symbolicTypeDefinition.getName());
                case 1:
                    resultBuilder = new ScalarResultBuilder(notEmptyReturnDefinitions);
                    break;
                default:
                    resultBuilder = new ArrayResultBuilder(notEmptyReturnDefinitions, rowColumnExtractor.getReturnHeaderDefinition().getType());
            }
        }
        return resultBuilder;
    }

}
