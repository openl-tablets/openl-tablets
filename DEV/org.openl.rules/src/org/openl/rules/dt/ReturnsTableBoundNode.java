package org.openl.rules.dt;

import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.DTColumnsDefinitionType;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

/**
 * @author Marat Kamalov
 *
 */
public class ReturnsTableBoundNode extends ADtColumnsDefinitionTableBoundNode {

    public ReturnsTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl) {
        super(tableSyntaxNode, openl);
    }

    @Override
    protected DTColumnsDefinition createDefinition(Map<String, List<IParameterDeclaration>> localParameters,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        return new DTColumnsDefinition(DTColumnsDefinitionType.RETURN,
            localParameters,
            header,
            compositeMethod,
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