/*
 * Created on Jun 23, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * The purpose of this class is to simplify compiling of OpenL objects in complex structured environments where context
 * is defined on top and must be propagated down without having to transfer many of the elements required to do the
 * validation and compilation.
 *
 * @author snshor
 */
public class OpenlToolAdaptor {

    private OpenL openl;
    private IOpenMethodHeader header;
    private IBindingContext bindingContext;
    private TableSyntaxNode tableSyntaxNode;

    public OpenlToolAdaptor(OpenL openl, IBindingContext bindingContext, TableSyntaxNode tableSyntaxNode) {
        this.openl = openl;
        this.bindingContext = bindingContext;
        this.tableSyntaxNode = tableSyntaxNode;
    }

    public IBindingContext getBindingContext() {
        return bindingContext;
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    public OpenL getOpenl() {
        return openl;
    }

    public void setHeader(IOpenMethodHeader header) {
        this.header = header;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

    public CompositeMethod makeMethod(IOpenSourceCodeModule src) {
        return OpenLCellExpressionsCompiler.makeMethod(openl, src, header, bindingContext);
    }

    public CompositeMethod makeMethod(IOpenSourceCodeModule src, IOpenMethodHeader h2) {
        return OpenLCellExpressionsCompiler.makeMethod(openl, src, h2, bindingContext);
    }

}
