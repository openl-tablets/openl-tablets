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
    private SpreadsheetOpenClass spreadsheetType;
    
    public SpreadsheetBuilder(IBindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }
    
    public void setSpreadsheetCellsBuilder(SpreadsheetStructureBuilder cellsBuilder) {
        this.structureBuilder = cellsBuilder;
    }
    
    /**
     * 
     * @param spreadsheetHeader
     */
    public void populateSpreadsheetOpenClass(IOpenMethodHeader spreadsheetHeader) {
        structureBuilder.addCellFields(getSpreadsheetOpenClass(spreadsheetHeader.getName()), spreadsheetHeader);
    }

    public void build(Spreadsheet spreadsheet) {   
        spreadsheet.setRowNames(structureBuilder.getRowNames());
        
        spreadsheet.setColumnNames(structureBuilder.getColumnNames());

        spreadsheet.setCells(structureBuilder.getCells(spreadsheet.getHeader()));
        
        spreadsheet.setResultBuilder(structureBuilder.getResultBuilder(spreadsheet));        
    }
    
    /**
     * Creates the spreadsheet open class
     * 
     * @param spreadsheetName name of the spreadsheet table
     * @return spreadsheet open class
     */
    public SpreadsheetOpenClass getSpreadsheetOpenClass(String spreadsheetName) {
        if (spreadsheetType == null) {
            spreadsheetType = new SpreadsheetOpenClass(null, spreadsheetName + "Type", bindingContext.getOpenL());
        }
        
        return spreadsheetType;
    }
}
