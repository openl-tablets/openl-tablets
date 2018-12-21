package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

// TODO: refactor
// Extract all the binding and build code to the SpreadsheetBinder
public class SpreadsheetBoundNode extends AMethodBasedNode implements IMemberBoundNode {

    private SpreadsheetStructureBuilder structureBuilder;
    private SpreadsheetOpenClass spreadsheetOpenClass;

    IBindingContext bindingContext;

    public SpreadsheetBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IOpenMethodHeader header,
            ModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    protected Spreadsheet createSpreadsheet() {
        /*
         * We need to generate a customSpreadsheet class only if return type of
         * the spreadsheet is SpreadsheetResult and the customspreadsheet
         * property is true
         */
        boolean isCustomSpreadsheetType = SpreadsheetResult.class.equals(getType().getInstanceClass()) && (!(getType() instanceof CustomSpreadsheetResultOpenClass)) && OpenLSystemProperties.isCustomSpreadsheetType(bindingContext
            .getExternalParams());

        return new Spreadsheet(getHeader(), this, isCustomSpreadsheetType);
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        Spreadsheet spreadsheet = createSpreadsheet();
        spreadsheet.setSpreadsheetType(spreadsheetOpenClass);
        // As custom spreadsheet result is being generated at runtime,
        // call this method to ensure that CSR will be generated during the
        // compilation.
        // Add generated type to be accessible through binding context.
        //
        spreadsheet.setRowNames(structureBuilder.getRowNames());
        spreadsheet.setColumnNames(structureBuilder.getColumnNames());

        spreadsheet.setRowTitles(structureBuilder.getRowTitles());
        spreadsheet.setColumnTitles(structureBuilder.getColumnTitles());
        if (spreadsheet.isCustomSpreadsheetType()) {
            
            IOpenClass type = null;
            try {
                type = spreadsheet.getType(); // Can throw RuntimeException
                bindingContext.addType(ISyntaxConstants.THIS_NAMESPACE, type);
            } catch (Exception e) {
                String message = String.format("Can't define type %s",
                    type != null ? type.getName() : spreadsheet.getName());
                SyntaxNodeException exception = SyntaxNodeExceptionUtils.createError(message, e, getTableSyntaxNode());
                getTableSyntaxNode().addError(exception);
                bindingContext.addError(exception);
            }
        }

        return spreadsheet;
    }

    public void preBind(IBindingContext bindingContext) throws SyntaxNodeException {
        if (!bindingContext.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new SpreadsheetMetaInfoReader(this));
        }

        TableSyntaxNode tableSyntaxNode = getTableSyntaxNode();
        validateTableBody(tableSyntaxNode, bindingContext);
        IOpenMethodHeader header = getHeader();

        this.bindingContext = bindingContext;
        Boolean autoType = tableSyntaxNode.getTableProperties().getAutoType();
        structureBuilder = new SpreadsheetStructureBuilder(tableSyntaxNode, bindingContext, header, autoType);
        String headerType = header.getName() + "Type";
        OpenL openL = bindingContext.getOpenL();
        spreadsheetOpenClass = new SpreadsheetOpenClass(headerType, openL);
        structureBuilder.addCellFields(spreadsheetOpenClass);
    }

    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        super.finalizeBind(bindingContext);

        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);

        Spreadsheet spreadsheet = getSpreadsheet();
        spreadsheet.setCells(structureBuilder.getCells());

        spreadsheet.setResultBuilder(structureBuilder.getResultBuilder(spreadsheet));
    }

    private void validateTableBody(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext) throws SyntaxNodeException {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError("Table has no body! Try to merge header cell horizontally to identify table.",
                getTableSyntaxNode());
        }

        int height = tableBody.getHeight();
        int width = tableBody.getWidth();

        if (height < 2 || width < 2) {
            String message = "Spreadsheet has empty body. Spreadsheet table should has at least 2x3 cells.";
            BindHelper.processWarn(message, tableSyntaxNode, bindingContext);
        }
    }

    private Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getMethod();
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

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        if (cxt.isExecutionMode()) {
            super.removeDebugInformation(cxt);
            // clean the builder, that was used for creating spreadsheet
            //
            this.structureBuilder.getSpreadsheetStructureBuilderHolder().clear();
            this.bindingContext = null;
        }
    }
}
