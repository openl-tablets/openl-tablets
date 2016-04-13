package org.openl.extension.xmlrules.binding;

import java.util.Arrays;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MethodBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class MethodWithAttributesBoundNode extends MethodBoundNode {
    private final IMethodCaller modifyContext;
    private final IMethodCaller restoreContext;
    private final List<Attribute> attributes;
    private final int parameterCount;

    public MethodWithAttributesBoundNode(ISyntaxNode syntaxNode,
            IBoundNode[] child,
            IMethodCaller methodCaller,
            IMethodCaller modifyContext,
            IMethodCaller restoreContext, List<Attribute> attributes, int parameterCount) {
        super(syntaxNode, child, methodCaller);
        this.modifyContext = modifyContext;
        this.restoreContext = restoreContext;
        this.attributes = attributes;
        this.parameterCount = parameterCount;
    }

    @Override
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);
        Object[] arguments = evaluateChildren(env);
        Object[] methodParameters = Arrays.copyOfRange(arguments, 0, parameterCount);
        Object[] attributeParameters = Arrays.copyOfRange(arguments, parameterCount, arguments.length);

        int attributesChanged = 0;
        try {
            // Modify runtime context
            for (int i = 0; i < attributes.size(); i++) {
                String attributeName = attributes.get(i).getName();
                Object attributeValue = convertAttribute(attributeName, attributeParameters[i]);
                Object[] params = new Object[] { attributeName, attributeValue };
                modifyContext.invoke(target, params, env);
                attributesChanged++;
            }

            // Invoke the function with modified context
            return getMethodCaller().invoke(target, methodParameters, env);
        } finally {
            // Restore runtime context
            for (int i = 0; i < attributesChanged; i++) {
                restoreContext.invoke(target, new Object[0], env);
            }
        }
    }

    private Object convertAttribute(String attributeName, Object attributeValue) {
        List<TablePropertyDefinition> dimensionalTableProperties = TablePropertyDefinitionUtils.getDimensionalTableProperties();
        for (TablePropertyDefinition property : dimensionalTableProperties) {
            String contextAttribute = property.getExpression().getMatchExpression().getContextAttribute();
            if (contextAttribute.equals(attributeName)) {
                IOpenClass type = property.getType();
                if (type.isArray()) {
                    type = type.getComponentClass();
                }
                IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(type.getInstanceClass());
                String argument = HelperFunctions.convertArgument(String.class, attributeValue);
                return converter.parse(argument, property.getFormat());
            }
        }

        // Couldn't find property and converter for it
        return attributeValue;
    }
}
