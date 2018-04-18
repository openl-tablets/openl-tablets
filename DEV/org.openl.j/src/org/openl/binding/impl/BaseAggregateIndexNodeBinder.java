package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.IndexParameterDeclarationBinder.IndexParameterNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;

/**
 * This the base class for a set of classes providing aggregate functions like SELECT FIRST, SELECT ALL, ORDER BY etc
 *
 * @author Yury Molchan
 */
public abstract class BaseAggregateIndexNodeBinder extends ANodeBinder {

    public abstract String getDefaultTempVarName(IBindingContext bindingContext);

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        BindHelper.processError("This node always binds  with target", node, bindingContext);

        return new ErrorBoundNode(node);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode) {

        try {
            bindingContext.pushLocalVarContext();

            IOpenClass containerType = targetNode.getType();
            IAggregateInfo info = containerType.getAggregateInfo();
            IOpenClass componentType = info.getComponentType(containerType);
            if (componentType == null) {
                String typeName = containerType.getName();
                throw SyntaxNodeExceptionUtils.createError(
                    "An array or a collection is expected, but " + typeName + " type has been defined.",
                    targetNode.getSyntaxNode());
            }

            // there could be 1 or 2 syntax nodes as children
            // If there is one syntax node, we use auto-generated local variable
            // if there is two syntax nodes, the first defines new local variable

            String varName;
            IOpenClass varType;
            ISyntaxNode expressionNode;
            switch (node.getNumberOfChildren()) {
                case 1:
                    expressionNode = node.getChild(0);

                    String defaultName = getDefaultTempVarName(bindingContext);
                    varName = BindHelper
                        .getTemporaryVarName(bindingContext, ISyntaxConstants.THIS_NAMESPACE, defaultName);
                    varType = componentType;

                    break;
                case 2:
                    expressionNode = node.getChild(1);
                    ISyntaxNode varNode = node.getChild(0);

                    IBoundNode localVarDefinitionBoundNode = bindChildNode(varNode, bindingContext);

                    IndexParameterNode pnode = (IndexParameterNode) localVarDefinitionBoundNode;

                    varName = pnode.getName();
                    varType = pnode.getType() == null ? componentType : pnode.getType();

                    if (varType != componentType) {
                        IOpenCast cast = bindingContext.getCast(componentType, varType);
                        if (cast == null) {
                            throw SyntaxNodeExceptionUtils
                                .createError("Can not cast " + componentType + " to " + varType, varNode);
                        }
                    }

                    break;
                default:
                    throw SyntaxNodeExceptionUtils.createError("Aggregate node can have either 1 or 2 childen nodes",
                        node);

            }

            ILocalVar localVar = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, varName, varType);
            TypeBindingContext varBindingContext = TypeBindingContext.create(bindingContext, localVar);
            IBoundNode boundExpressionNode = bindChildNode(expressionNode, varBindingContext);

            boundExpressionNode = validateExpressionNode(boundExpressionNode, bindingContext);

            IBoundNode boundNode = createBoundNode(node, targetNode, boundExpressionNode, localVar);
            return boundNode;
        } catch (SyntaxNodeException error) {
            bindingContext.addError(error);
            return new ErrorBoundNode(node);
        } finally {
            bindingContext.popLocalVarContext();
        }

    }

    protected abstract IBoundNode createBoundNode(ISyntaxNode node,
            IBoundNode targetNode,
            IBoundNode expressionNode,
            ILocalVar localVar);

    protected abstract IBoundNode validateExpressionNode(IBoundNode expressionNode, IBindingContext bindingContext);

}
