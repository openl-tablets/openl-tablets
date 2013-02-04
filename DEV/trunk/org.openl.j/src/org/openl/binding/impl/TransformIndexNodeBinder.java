package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Binds conditional index for arrays like:
 * - arrayOfDrivers[@ age  < 20];
 * - arrayOfDrivers[select all having  gender == "Male"]     
 * 
 * @author PUdalau
 */
public class TransformIndexNodeBinder extends BaseAggregateIndexNodeBinder {

    private static final String TEMPORARY_VAR_NAME = "selectAllIndex";

    private static class TransformIndexNode extends ABoundNode {
        private ILocalVar tempVar;
        private boolean isUnique;

        public TransformIndexNode(ISyntaxNode syntaxNode, IBoundNode[] children,ILocalVar tempVar, boolean isUnique) {
            super(syntaxNode, children);
            this.tempVar = tempVar;
            this.isUnique = isUnique;
        }

        public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
            IBoundNode container = getContainer();
            IBoundNode transformer = getTransformer();
            IAggregateInfo aggregateInfo = getType().getAggregateInfo();
            Iterator<Object> elementsIterator = container.getType().getAggregateInfo().getIterator(container.evaluate(env));
            List<Object> firedElements = new ArrayList<Object>();
            HashSet<Object> uniqueSet = null;
            if (isUnique)
            	uniqueSet = new HashSet<Object>();
            while (elementsIterator.hasNext()) {
                Object element = elementsIterator.next();
                tempVar.set(null, element, env);
                Object transformed = transformer.evaluate(env); 
                if (isUnique)
                {	
                	if (uniqueSet.add(transformed))
                       firedElements.add(transformed);
                }
                else 
                    firedElements.add(transformed);
                
            }
            Object result = aggregateInfo.makeIndexedAggregate(transformer.getType(),
                    new int[] { firedElements.size() });
            for (int i = 0; i < firedElements.size(); i++) {
                Array.set(result, i, firedElements.get(i));
            }
            return result;
        }

        private IBoundNode getContainer() {
            return getChildren()[0];
        }
        
        private IBoundNode getTransformer() {
            return getChildren()[1];
        }
        

        public IOpenClass getType() {
        	IOpenClass containerType = getContainer().getType();
        	if (containerType.isArray())
        	{	
        		IAggregateInfo info = getContainer().getType().getAggregateInfo();
        		return info.getIndexedAggregateType(getTransformer().getType(), 1);
        	}
        	
        	IOpenClass componentType = tempVar.getType();
    		return componentType.getAggregateInfo().getIndexedAggregateType(getTransformer().getType(), 1);
        }
    }

//    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
//        BindHelper.processError("This node always binds  with target", node, bindingContext);
//
//        return new ErrorBoundNode(node);
//    }
//
//    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode)
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
//        IBoundNode transformNode = children[0];
//        
//        boolean isUnique = node.getType().contains("unique");
//        
//        return new TransformIndexNode(node, new IBoundNode[] { targetNode, transformNode}, var, isUnique);
//    }
    
	@Override
	public String getDefaultTempVarName(IBindingContext bindingContext) {
		return BindHelper.getTemporaryVarName(bindingContext,
				ISyntaxConstants.THIS_NAMESPACE, TEMPORARY_VAR_NAME);
	}

	@Override
	protected IBoundNode createBoundNode(ISyntaxNode node,
			IBoundNode targetNode, IBoundNode expressionNode, ILocalVar localVar) {
		boolean isUnique = node.getType().contains("unique");
		return new TransformIndexNode(node, new IBoundNode[] {
				targetNode, expressionNode }, localVar, isUnique);
	}

	@Override
	protected IBoundNode validateExpressionNode(IBoundNode expressionNode,
			IBindingContext bindingContext) {
		return expressionNode;
	}

    
}
