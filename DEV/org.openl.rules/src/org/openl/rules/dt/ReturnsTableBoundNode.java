package org.openl.rules.dt;

import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.DTColumnsDefinitionType;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;

/**
 * @author Marat Kamalov
 *
 */
public class ReturnsTableBoundNode extends ADtColumnsDefinitionTableBoundNode {

    public ReturnsTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl, IBindingContext bindingContext) {
        super(tableSyntaxNode, openl, bindingContext);
    }

    @Override
    protected DTColumnsDefinition createDefinition(IOpenMethodHeader header,
            String expression,
            Map<String, List<IParameterDeclaration>> parameters) {
        return new DTColumnsDefinition(DTColumnsDefinitionType.RETURN,
            getTableName(),
            header,
            expression,
            parameters,
            getTableSyntaxNode());
    }

    @Override
    protected boolean isConditions() {
        return false;
    }

    @Override
    protected boolean isActions() {
        return false;
    }

    @Override
    protected boolean isReturns() {
        return true;
    }

}