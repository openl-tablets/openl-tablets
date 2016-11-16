package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;

/**
 * TODO: refactor
 * 
 * @author DLiauchuk
 *
 */
public class SpreadsheetBuilder {

    private SpreadsheetStructureBuilder structureBuilder;

    private SpreadsheetOpenClass spreadsheetOpenClass;

    public SpreadsheetBuilder(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext, IOpenMethodHeader header) {
        OpenL openl = bindingContext.getOpenL();
        String type = header.getName() + "Type";
        this.spreadsheetOpenClass = new SpreadsheetOpenClass(type, openl);
        Boolean autoType = tableSyntaxNode.getTableProperties().getAutoType();
        this.structureBuilder = new SpreadsheetStructureBuilder(tableSyntaxNode, bindingContext, header, autoType);
    }

    /**
     * See
     * {@link SpreadsheetStructureBuilder#addCellFields(SpreadsheetOpenClass)}
     */
    public void populateSpreadsheetOpenClass() {
        structureBuilder.addCellFields(spreadsheetOpenClass);
    }
    
    public void populateRowAndColumnNames(Spreadsheet spreadsheet) {
        spreadsheet.setRowNames(structureBuilder.getRowNames());

        spreadsheet.setColumnNames(structureBuilder.getColumnNames());
    }

    public void finalizeBuild(Spreadsheet spreadsheet) {
        spreadsheet.setCells(structureBuilder.getCells());

        spreadsheet.setResultBuilder(structureBuilder.getResultBuilder(spreadsheet));
    }

    public void removeDebugInformation() throws Exception {
        structureBuilder.getSpreadsheetStructureBuilderHolder().clear();
    }

    public SpreadsheetOpenClass getPopulatedSpreadsheetOpenClass() {
        return spreadsheetOpenClass;
    }
}
