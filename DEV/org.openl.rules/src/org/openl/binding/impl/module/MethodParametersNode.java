package org.openl.binding.impl.module;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.util.text.ILocation;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class MethodParametersNode extends ABoundNode {

    public MethodParametersNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    public IMethodSignature getSignature(IBindingContext bindingContext) {
        int len = children.length;

        ParameterDeclaration[] params = new ParameterDeclaration[len];
        Map<String, Integer> checkConflicts = new HashMap<>();
        for (int i = 0; i < len; i++) {
            if (children[i] instanceof ParameterNode) {
                ParameterNode parameterNode = (ParameterNode) children[i];
                params[i] = new ParameterDeclaration(parameterNode.getType(),
                        parameterNode.getName(),
                        parameterNode.getContextProperty());
                if (parameterNode.getContextProperty() != null) {
                    checkConflicts.merge(parameterNode.getContextProperty(), 1, Integer::sum);
                }
            } else {
                params[i] = new ParameterDeclaration(children[i].getType(), null, null, null);
            }
        }
        checkConflicts.entrySet().stream().filter(e -> e.getValue() > 1).forEach(e -> {
            bindingContext.addError(SyntaxNodeExceptionUtils.createError(
                    String.format("Multiple method parameters refer to the same context property '%s'.", e.getKey()),
                    getSyntaxNode()));
        });
        return new MethodSignature(params);

    }

    public ILocation getParamTypeLocation(int paramNum) {
        // 0-th child is param type, 1-st child is param name. See ParameterDeclarationNodeBinder
        ISyntaxNode typeNode = children[paramNum].getSyntaxNode().getChild(0);

        while (typeNode.getNumberOfChildren() == 1 && !(typeNode instanceof IdentifierNode)) {
            // Get type node for array
            typeNode = typeNode.getChild(0);
        }
        return typeNode.getSourceLocation();
    }

    @Override
    public IOpenClass getType() {
        return NullOpenClass.the;
    }

}
