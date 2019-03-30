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
public class ConditionsTableBoundNode extends ADtColumnsDefinitionTableBoundNode {

    public ConditionsTableBoundNode(TableSyntaxNode tableSyntaxNode, OpenL openl) {
        super(tableSyntaxNode, openl);
    }

    @Override
    protected void createAndAddDefinition(Map<String, List<IParameterDeclaration>> localParameters,
            IOpenMethodHeader header,
            CompositeMethod compositeMethod) {
        DTColumnsDefinition conditionDefinition = new DTColumnsDefinition(DTColumnsDefinitionType.CONDITION,
            localParameters,
            header,
            compositeMethod);
        getXlsModuleOpenClass().getXlsDefinitions().addDtColumnsDefinition(conditionDefinition);
    }

}