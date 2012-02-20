package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.types.IOpenMethodHeader;

/**
 * TODO: refactor
 * @author DLiauchuk
 *
 */
public class SpreadsheetBuilder {

    private SpreadsheetStructureBuilder structureBuilder;
    
    private IBindingContext bindingContext;    
    private SpreadsheetOpenClass spreadsheetOpenClass;
    
    public SpreadsheetBuilder(IBindingContext bindingContext, SpreadsheetOpenClass spreadsheetOpenClass) {
        this.bindingContext = bindingContext;
        this.spreadsheetOpenClass = spreadsheetOpenClass;
    }
    
    public void setSpreadsheetCellsBuilder(SpreadsheetStructureBuilder cellsBuilder) {
        this.structureBuilder = cellsBuilder;
    }
    
    public IBindingContext getBindingContext() {
        return bindingContext;
    }
    
    /**
     * See {@link SpreadsheetStructureBuilder#addCellFields(SpreadsheetOpenClass, IOpenMethodHeader)}
     * @param spreadsheetHeader
     */
    public void populateSpreadsheetOpenClass(IOpenMethodHeader spreadsheetHeader) {
        structureBuilder.addCellFields(spreadsheetOpenClass, spreadsheetHeader.getType());
    }

    public void finalizeBuild(Spreadsheet spreadsheet) {   
        spreadsheet.setRowNames(structureBuilder.getRowNames());
        
        spreadsheet.setColumnNames(structureBuilder.getColumnNames());

        spreadsheet.setCells(structureBuilder.getCells(spreadsheet.getHeader()));
        
        spreadsheet.setResultBuilder(structureBuilder.getResultBuilder(spreadsheet));        
    }
    
    public SpreadsheetOpenClass getPopulatedSpreadsheetOpenClass() {
    	return spreadsheetOpenClass;
    }
}
