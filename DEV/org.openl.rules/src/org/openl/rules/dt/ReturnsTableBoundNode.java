package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.DTColumnDefinition;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

/**
 * @author Marat Kamalov
 * 
 */
public class ReturnsTableBoundNode extends ADefinitionTableBoundNode {

    public ReturnsTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl) {
        super(tableSyntaxNode, openl, true);
    }

    protected void createAndAddDefinition(String[] titles,
            IParameterDeclaration[] parameterDeclarations,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        DTColumnDefinition returnDefinition = new DTColumnDefinition(titles,
            parameterDeclarations,
            header,
            compositeMethod);
        getXlsModuleOpenClass().getXlsDefinitions().addReturnDefinition(returnDefinition);
    }

}