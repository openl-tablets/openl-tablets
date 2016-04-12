package org.openl.extension.xmlrules.binding;

import java.util.Arrays;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MethodBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.rules.binding.RulesModuleBindingContext;
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
                Object[] params = new Object[] {attributes.get(i).getName(), attributeParameters[i]};
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
}
