package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

public class SpreadsheetBoundNode extends AMethodBasedNode implements IMemberBoundNode {
    
    private SpreadsheetBuilder builder;
    
    public SpreadsheetBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IOpenMethodHeader header,
            ModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        Spreadsheet spreadsheet = new Spreadsheet(getHeader(), this);
        spreadsheet.setSpreadsheetType(builder.getSpreadsheetOpenClass(spreadsheet.getName()));
        return spreadsheet;
    }
    
    private void initSpreadsheetBuilder(IBindingContext bindingContext) throws Exception {
        validateTableBody(getTableSyntaxNode().getTableBody());
        this.builder = new SpreadsheetBuilder(bindingContext, getTableSyntaxNode());
    }
     
    public void preBind(IBindingContext bindingContext) throws Exception {
        initSpreadsheetBuilder(bindingContext);
        builder.firstBuildPhase(getHeader());
    }
    
    public void finalizeBind(IBindingContext bindingContext) throws Exception {        
        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();

        

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);
        
        builder.secondBuildPhase(getSpreadsheet());
    }

    private void validateTableBody(ILogicalTable tableBody) throws SyntaxNodeException {
        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError("Table has no body! Try to merge header cell horizontally to identify table.",
                getTableSyntaxNode());
        }

        int height = tableBody.getHeight();
        int width = tableBody.getWidth();

        if (height < 2 || width < 2) {
            String message = String.format("Spreadsheet must have at least 2x2 cells! Actual size %dx%d.",
                width,
                height);

            throw SyntaxNodeExceptionUtils.createError(message, getTableSyntaxNode());
        }
    }

    public Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getMethod();
    }
    
    public SpreadsheetBuilder getSpreadsheetBuilder() {
        return builder;
    }
    
    @Override
    public void updateDependency(BindingDependencies dependencies) {   
        if (getSpreadsheet().getCells() != null) {
            for (SpreadsheetCell[] cellArray : getSpreadsheet().getCells()) {
                if (cellArray != null) {
                    for (SpreadsheetCell cell : cellArray) {
                        if (cell != null) {
                            CompositeMethod method = (CompositeMethod) cell.getMethod();
                            if (method != null) {
                                method.updateDependency(dependencies);
                            }
                        }
                    }
                }
            }
        }
    }

}
