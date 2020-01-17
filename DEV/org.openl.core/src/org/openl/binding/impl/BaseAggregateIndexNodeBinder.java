package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.IndexParameterDeclarationBinder.IndexParameterNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;

/**
 * This the base class for a set of classes providing aggregate functions like SELECT FIRST, SELECT ALL, ORDER BY etc
 *
 * @author Yury Molchan
 */
public abstract class BaseAggregateIndexNodeBinder extends ANodeBinder {

    /**
     * Analyzes the binding context and returns the name for internal/temporary/service variable with the name:
     * varNamePrefix + '$' + available_index.
     */
    private static String getTemporaryVarName(IBindingContext bindingContext) {
        int index = 0;
        String tmpVarName = "tmp$";
        while (bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, tmpVarName, true) != null) {
            tmpVarName = "tmp$" + index;
            index++;
        }
        return tmpVarName;
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return makeErrorNode("This node always binds with target", node, bindingContext);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode) {

        IOpenClass containerType = targetNode.getType();

        if (NullOpenClass.the.equals(containerType)) {
            return makeErrorNode(String.format("An array or a collection is expected, but type '%s' is found.",
                NullOpenClass.the.getName()), targetNode.getSyntaxNode(), bindingContext);
        }

        IAggregateInfo info = containerType.getAggregateInfo();
        IOpenClass componentType = info.getComponentType(containerType);
        if (componentType == null) {
            String typeName = containerType.getName();
            return makeErrorNode(String.format("An array or a collection is expected, but type '%s' is found.",
                typeName), targetNode.getSyntaxNode(), bindingContext);
        }
        int numberOfChildren = node.getNumberOfChildren();
        if (numberOfChildren < 1 || numberOfChildren > 2) {
            return makeErrorNode("Aggregate node can have either 1 or 2 childen nodes.", node, bindingContext);
        }

        // there could be 1 or 2 syntax nodes as children
        // If there is one syntax node, we use auto-generated local variable
        // if there is two syntax nodes, the first defines new local variable

        String varName;
        IOpenClass varType;
        ISyntaxNode expressionNode;
        if (numberOfChildren == 1) {
            expressionNode = node.getChild(0);

            varName = getTemporaryVarName(bindingContext);
            varType = componentType;
        } else {
            expressionNode = node.getChild(1);
            ISyntaxNode varNode = node.getChild(0);

            IBoundNode localVarDefinitionBoundNode = bindChildNode(varNode, bindingContext);

            IndexParameterNode pnode = (IndexParameterNode) localVarDefinitionBoundNode;

            varName = pnode.getName();
            varType = pnode.getType() == null ? componentType : pnode.getType();

            if (varType != componentType) {
                IOpenCast cast = bindingContext.getCast(componentType, varType);
                if (cast == null) {
                    return makeErrorNode(String.format("Cannot cast '%s' to '%s'.", componentType, varType),
                        varNode,
                        bindingContext);
                }
            }
        }

        try {
            bindingContext.pushLocalVarContext();
            ILocalVar localVar = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, varName, varType);
            TypeBindingContext varBindingContext = TypeBindingContext.create(bindingContext, localVar);
            IBoundNode boundExpressionNode = bindChildNode(expressionNode, varBindingContext);
            if (boundExpressionNode instanceof TypeBoundNode) {
                String message = "Type definition cannot be used as expression for array index operator.";
                return makeErrorNode(message, boundExpressionNode.getSyntaxNode(), bindingContext);
            }
            return createBoundNode(node, targetNode, boundExpressionNode, localVar, bindingContext);
        } finally {
            bindingContext.popLocalVarContext();
        }

    }

    protected abstract IBoundNode createBoundNode(ISyntaxNode node,
            IBoundNode targetNode,
            IBoundNode expressionNode,
            ILocalVar localVar,
            IBindingContext bindingContext);
}
