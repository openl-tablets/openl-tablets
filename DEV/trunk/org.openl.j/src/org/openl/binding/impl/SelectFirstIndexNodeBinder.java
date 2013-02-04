package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Binds conditional index for arrays that returns the first matching element like:
 * - arrayOfDrivers[@ age  < 20];
 * - arrayOfDrivers[select all having  gender == "Male"]     
 * 
 * @author PUdalau
 */
public class SelectFirstIndexNodeBinder extends BaseAggregateIndexNodeBinder {

    private static final String TEMPORARY_VAR_NAME = "selectFirstIndex";

    private static class ConditionalSelectIndexNode extends ABoundNode {
        private ILocalVar tempVar;

        public ConditionalSelectIndexNode(ISyntaxNode syntaxNode, IBoundNode[] children, ILocalVar tempVar) {
            super(syntaxNode, children);
            this.tempVar = tempVar;
        }

        public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
            IBoundNode container = getContainer();
            IBoundNode condition = getChildren()[1];
            IAggregateInfo aggregateInfo = getContainer(). getType().getAggregateInfo();
            Iterator<Object> elementsIterator = aggregateInfo.getIterator(container.evaluate(env));
            while (elementsIterator.hasNext()) {
                Object element = elementsIterator.next();
                tempVar.set(null, element, env);
                if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                    return element;
                }
            }
            return null;
        }

        private IBoundNode getContainer() {
            return getChildren()[0];
        }

        public IOpenClass getType() {
            IOpenClass type = getContainer().getType();
            return type.getAggregateInfo().getComponentType(type);
        }
    }


//    public IBoundNode bindTargetZZZ(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode)
//            throws Exception {
//
//        if (node.getNumberOfChildren() != 1) {
//            BindHelper.processError("Index node must have  exactly 1 subnode", node, bindingContext);
//
//            return new ErrorBoundNode(node);
//        }
//
//        IOpenClass containerType = targetNode.getType();
//        IAggregateInfo info = containerType.getAggregateInfo();
//
//        String varName = BindHelper.getTemporaryVarName(bindingContext, ISyntaxConstants.THIS_NAMESPACE, TEMPORARY_VAR_NAME);
//        ILocalVar var = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE, varName, info.getComponentType(containerType));
//
//        IBoundNode[] children = bindChildren(node, new TypeBindingContext(bindingContext, var));
//        IBoundNode conditionNode = BindHelper.checkConditionBoundNode(children[0], bindingContext);
//        return new ConditionalSelectIndexNode(node, new IBoundNode[] { targetNode, conditionNode }, var);
//    }


	@Override
	public String getDefaultTempVarName(IBindingContext bindingContext) {
		return  BindHelper.getTemporaryVarName(bindingContext, ISyntaxConstants.THIS_NAMESPACE, TEMPORARY_VAR_NAME);
	}


	@Override
	protected IBoundNode createBoundNode(ISyntaxNode node,
			IBoundNode targetNode, IBoundNode expressionNode, ILocalVar localVar) {
        return new ConditionalSelectIndexNode(node, new IBoundNode[] { targetNode, expressionNode }, localVar);
	}


	@Override
	protected IBoundNode validateExpressionNode(IBoundNode expressionNode, IBindingContext bindingContext) {
		return BindHelper.checkConditionBoundNode(expressionNode, bindingContext);
	}
}
