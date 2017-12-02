package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Binds conditional index for arrays like: - arrayOfDrivers[@ age < 20]; -
 * arrayOfDrivers[select all having gender == "Male"]
 * 
 * @author PUdalau
 */
public class SplitByIndexNodeBinder extends BaseAggregateIndexNodeBinder {

	private static final String TEMPORARY_VAR_NAME = "SplitByIndex";


	private static class SplitByIndexNode extends ABoundNode {

		private ILocalVar tempVar;

		public SplitByIndexNode(ISyntaxNode syntaxNode, IBoundNode[] children,
				ILocalVar tempVar) {
			super(syntaxNode, children);
			this.tempVar = tempVar;
		}

		@Override
		protected Object evaluateRuntime(IRuntimeEnv env) {
			IBoundNode containerNode = getContainer();
			IBoundNode splitBy = getChildren()[1];
			IOpenClass containerType = containerNode.getType();
			IAggregateInfo aggregateInfo = containerType.getAggregateInfo();
			Object container = containerNode.evaluate(env);

			Iterator<Object> elementsIterator = aggregateInfo
					.getIterator(container);
			
			
			Object tempKey = new Object();

			HashMap<Object, ArrayList<Object>> map = new HashMap<Object, ArrayList<Object>>();
			ArrayList<ArrayList<Object>> list2d = new ArrayList<ArrayList<Object>>();
			

			while (elementsIterator.hasNext()) {
				Object element = elementsIterator.next();
				tempVar.set(null, element, env);
				Object key =  splitBy.evaluate(env);
				
				if (key == null)
					key = tempKey;
				
				ArrayList<Object> list = map.get(key);
				
				if (list == null)
				{
					list = new ArrayList<Object>();
					map.put(key, list);
					list2d.add(list);
				}
				
				list.add(element);
			}

			
			int size = list2d.size();
			
			IOpenClass componentType = tempVar.getType();
			IOpenClass arrayType = componentType.getAggregateInfo().getIndexedAggregateType(componentType, 1);
			
			Object result = componentType.getAggregateInfo().makeIndexedAggregate(
					arrayType,
					new int[] { size });
			

			for (int i = 0; i < size; i++) {
				
				ArrayList<Object> list = list2d.get(i);
				int listSize = list.size();
				
				Object ary = componentType.getAggregateInfo().makeIndexedAggregate(componentType, new int[]{listSize});

				for (int j = 0; j < listSize; j++) {
					Array.set(ary, j, list.get(j));
				}
				
				Array.set(result, i, ary);
				
				
			}
			

			return result;
		}
		

		private IBoundNode getContainer() {
			return getChildren()[0];
		}

		public IOpenClass getType() {
			
			IOpenClass containerType = getContainer().getType();
			if (containerType.isArray())
			{	
				IAggregateInfo info =  containerType.getAggregateInfo();
				return info.getIndexedAggregateType(containerType, 1);
			}
			
			
			IOpenClass componentType = tempVar.getType();
			IAggregateInfo info =  componentType.getAggregateInfo();
			return info.getIndexedAggregateType(componentType, 2);
			
		}
	}


	@Override
	public String getDefaultTempVarName(IBindingContext bindingContext) {
		return BindHelper.getTemporaryVarName(bindingContext,
				ISyntaxConstants.THIS_NAMESPACE, TEMPORARY_VAR_NAME);
	}

	@Override
	protected IBoundNode createBoundNode(ISyntaxNode node,
			IBoundNode targetNode, IBoundNode expressionNode, ILocalVar localVar) {
		return new SplitByIndexNode(node, new IBoundNode[] {
				targetNode, expressionNode }, localVar);
	}

	@Override
	protected IBoundNode validateExpressionNode(IBoundNode expressionNode,
			IBindingContext bindingContext) {
		return expressionNode;
	}


}
