package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.ReturnDefinition;
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
        super(tableSyntaxNode, openl, true, true);
    }

    protected void createAndAddDefinition(String[] parameterDescriptions,
            IParameterDeclaration[] parameterDeclarations,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        ReturnDefinition returnDefinition = new ReturnDefinition(parameterDescriptions,
            parameterDeclarations,
            header,
            compositeMethod);
        getXlsModuleOpenClass().getXlsDefinitions().addReturnDefinition(returnDefinition);
    }

}