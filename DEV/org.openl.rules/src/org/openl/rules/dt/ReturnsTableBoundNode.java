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
        super(tableSyntaxNode, openl, true);
    }

    protected void createAndAddDefinition(Map<String, List<IParameterDeclaration>> parameterDeclarations,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        DTColumnsDefinition returnDefinition = new DTColumnsDefinition(DTColumnsDefinitionType.RETURN,
            parameterDeclarations,
            header,
            compositeMethod);
        getXlsModuleOpenClass().getXlsDefinitions().addDtColumnsDefinition(returnDefinition);
    }

}