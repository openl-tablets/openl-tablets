package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;

/**
 * 
 * @author snshor
 * 
 *         This the base class for a set of classes providing aggregate
 *         functions like SELECT FIRST, SELECT ALL, ORDER BY etc
 */
public abstract class BaseAggregateIndexNodeBinder extends ANodeBinder {

	public abstract String getDefaultTempVarName(IBindingContext bindingContext);

	@Override
	public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
			throws Exception {
		BindHelper.processError("This node always binds  with target", node,
				bindingContext);

		return new ErrorBoundNode(node);
	}

	@Override
	public IBoundNode bindTarget(ISyntaxNode node,
			IBindingContext bindingContext, IBoundNode targetNode)
			throws Exception {

		try {
			bindingContext.pushLocalVarContext();
			ILocalVar localVar = prepareLocalVar(node, bindingContext, targetNode);
			IBoundNode[] children = bindAndValidateChildren(node,
					bindingContext, localVar);
			IBoundNode boundNode = createBoundNode(node, targetNode, children[0], localVar);
			return boundNode;
		} catch (SyntaxNodeException error) {
			bindingContext.addError(error);
			return new ErrorBoundNode(node);
		} finally {
			bindingContext.popLocalVarContext();
		}

	}

	protected abstract IBoundNode createBoundNode(ISyntaxNode node,
			IBoundNode targetNode, IBoundNode expressionNode, ILocalVar localVar);

	protected ILocalVar prepareLocalVar(ISyntaxNode node,
			IBindingContext bindingContext, IBoundNode targetNode) throws SyntaxNodeException {

		// there could be 1 or 2 syntax nodes as children
		// If there is one syntax node, we use auto-generated local variable
		// if there is two syntax nodes, the first defines new local variable

		switch (node.getNumberOfChildren()) {
		case 1:
			return prepareDefaultLocalVar(node, bindingContext, targetNode);
		case 2:
			return prepareDefinedLocalVar(node, bindingContext, targetNode);
		default:
			throw SyntaxNodeExceptionUtils.createError("Aggregate node can have either 1 or 2 childen nodes", node);
			
		}

	}

	private ILocalVar prepareDefinedLocalVar(ISyntaxNode node,
			IBindingContext bindingContext, IBoundNode targetNode) throws SyntaxNodeException {

		IOpenClass containerType = targetNode.getType();
		IAggregateInfo info = containerType.getAggregateInfo();
		IOpenClass componentType = info.getComponentType(containerType);	

		IBoundNode localVarDefinitionBoundNode = bindChildNode(node.getChild(0), bindingContext);
		
		IndexParameterDeclarationBinder.IndexParameterNode pnode = (IndexParameterDeclarationBinder.IndexParameterNode)localVarDefinitionBoundNode;
		
		String varName = pnode.getName();
		IOpenClass varType = pnode.getType() == null ? componentType : pnode.getType();
		
		if (varType != componentType)
		{
			IOpenCast cast = bindingContext.getCast(componentType, varType);
			if (cast == null)
	            BindHelper.processError("Can not cast " + componentType + " to " + varType, node.getChild(0), bindingContext, false);

		}	

		ILocalVar var = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE,
				varName, varType);
		
		return var;
	
	}

	protected ILocalVar prepareDefaultLocalVar(ISyntaxNode node,
			IBindingContext bindingContext, IBoundNode targetNode) {
		IOpenClass containerType = targetNode.getType();
		IAggregateInfo info = containerType.getAggregateInfo();

		String varName = BindHelper.getTemporaryVarName(bindingContext,
				ISyntaxConstants.THIS_NAMESPACE, getDefaultTempVarName(bindingContext));
		ILocalVar var = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE,
				varName, info.getComponentType(containerType));

		return var;

	}

	private IBoundNode[] bindAndValidateChildren(ISyntaxNode node,
			IBindingContext bindingContext, ILocalVar localVar)
			throws SyntaxNodeException {

		
		ISyntaxNode expressionNode =  node.getNumberOfChildren() == 1 ? node.getChild(0) : node.getChild(1);
			
		IBoundNode boundExpressionNode = bindChildNode(expressionNode,  TypeBindingContext.create(
				bindingContext, localVar));

		boundExpressionNode = validateExpressionNode(boundExpressionNode, bindingContext);
		
		return new IBoundNode[]{boundExpressionNode};
	}

	protected abstract IBoundNode validateExpressionNode(IBoundNode expressionNode, IBindingContext bindingContext);

}
