package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

public class SpreadsheetBoundNode extends AMethodBasedNode implements IMemberBoundNode {

    public SpreadsheetBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IOpenMethodHeader header,
            ModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    @Override
    protected IOpenMethod createMethodShell() {
        return new Spreadsheet(getHeader(), this);
    }

    public void finalizeBind(IBindingContext bindingContext) throws Exception {

        SpreadsheetBuilder builder = new SpreadsheetBuilder(bindingContext, getSpreadsheet(), getTableSyntaxNode());
        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();

        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError("Table has no body! Try to merge header cell horizontally to identify table.",
                getTableSyntaxNode());
        }

        int height = tableBody.getLogicalHeight();
        int width = tableBody.getLogicalWidth();

        if (height < 2 || width < 2) {
            String message = String.format("Spreadsheet must have at least 2x2 cells! Actual size %dx%d.",
                width,
                height);

            throw SyntaxNodeExceptionUtils.createError(message, getTableSyntaxNode());
        }

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);

        builder.build(tableBody);
        if (bindingContext.isExecutionMode()) {
            getSpreadsheet().setBoundNode(null);
        }
    }

    public Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getMethod();
    }

}
