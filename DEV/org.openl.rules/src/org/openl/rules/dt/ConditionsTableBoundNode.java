package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.ConditionDefinition;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;

/**
 * @author Marat Kamalov
 * 
 */
public class ConditionsTableBoundNode extends ADefinitionTableBoundNode {

    public ConditionsTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl) {
        super(tableSyntaxNode, openl, false, false);
    }
    
    protected void createAndAddDefinition(String[] titles, IParameterDeclaration[] parameterDeclarations, IOpenMethodHeader header, CompositeMethod compositeMethod) {
        ConditionDefinition conditionDefinition = new ConditionDefinition(titles,
            parameterDeclarations,
            header,
            compositeMethod);
        getXlsModuleOpenClass().getXlsDefinitions().addConditionDefinition(conditionDefinition);
    }

}