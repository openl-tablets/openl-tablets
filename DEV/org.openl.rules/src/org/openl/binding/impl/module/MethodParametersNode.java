package org.openl.binding.impl.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
        var len = children.length;

        ParameterDeclaration[] params = new ParameterDeclaration[len];
        var checkConflicts = new HashMap<String, Integer>();
        var names = new HashSet<String>();
        for (var i = 0; i < len; i++) {
            if (children[i] instanceof ParameterNode parameterNode) {
                params[i] = new ParameterDeclaration(parameterNode.getType(),
                        uniqueName(parameterNode, names, bindingContext),
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
                    "Multiple method parameters refer to the same context property '%s'.".formatted(e.getKey()),
                    getSyntaxNode()));
        });
        return new MethodSignature(params);

    }

    /**
     * The name of the parameter, or {@code null} for a name an earlier parameter already declared.
     *
     * <p>A name declared twice is reported once, at the parameter declaring it again. The parameter is left without a
     * name, as one whose name cannot be read is, so the method is not built and none of its cells repeats the error.
     */
    private static String uniqueName(ParameterNode parameterNode, Set<String> names, IBindingContext bindingContext) {
        var name = parameterNode.getName();
        if (name == null || names.add(name)) {
            return name;
        }
        bindingContext.addError(SyntaxNodeExceptionUtils.createError(
                "Parameter '%s' is already defined.".formatted(name), parameterNode.getSyntaxNode()));
        return null;
    }

    public ILocation getParamTypeLocation(int paramNum) {
        // 0-th child is param type, 1-st child is param name. See ParameterDeclarationNodeBinder
        var typeNode = children[paramNum].getSyntaxNode().getChild(0);

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
