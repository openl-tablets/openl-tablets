package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

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
 * Binds conditional index for arrays like: - arrayOfDrivers[@ age < 20]; -
 * arrayOfDrivers[select all having gender == "Male"]
 * 
 * @author PUdalau
 */
public class OrderByIndexNodeBinder extends ANodeBinder {

	private static final String TEMPORARY_VAR_NAME = "OrderByIndex";

	private static class OrderList extends ArrayList<Object> {
		private static final long serialVersionUID = 1L;
	}

	private static class OrderByIndexNode extends ABoundNode {

		private ILocalVar tempVar;
		private boolean isDecreasing;

		public OrderByIndexNode(ISyntaxNode syntaxNode, IBoundNode[] children,
				ILocalVar tempVar, boolean isDecreasing) {
			super(syntaxNode, children);
			this.tempVar = tempVar;
			this.isDecreasing = isDecreasing;
		}

		public Object evaluateRuntime(IRuntimeEnv env)
				throws OpenLRuntimeException {
			IBoundNode containerNode = getContainer();
			IBoundNode orderBy = getChildren()[1];
			IAggregateInfo aggregateInfo = getType().getAggregateInfo();
			Object container = containerNode.evaluate(env);

			Iterator<Object> elementsIterator = aggregateInfo
					.getIterator(container);

			TreeMap<Comparable<?>, Object> map = new TreeMap<Comparable<?>, Object>();

			int size = 0;
			while (elementsIterator.hasNext()) {
				Object element = elementsIterator.next();
				tempVar.set(null, element, env);
				Comparable<?> key = (Comparable<?>) orderBy.evaluate(env);
				Object prev = map.put(key, element);
				if (prev != null) {
					OrderList list = null;
					if (prev.getClass() != OrderList.class) {
						list = new OrderList();
						list.add(prev);
					} else
						list = (OrderList) prev;

					list.add(element);
					map.put(key, list);
				}
				++size;
			}

			Object result = aggregateInfo.makeIndexedAggregate(
					aggregateInfo.getComponentType(getType()),
					new int[] { size });

			Iterator<Object> mapIterator = map.values().iterator();
			int idx = 0;
			while (mapIterator.hasNext()) {
				Object element = mapIterator.next();
				if (element.getClass() != OrderList.class)
					Array.set(result, nextIdx( idx++, size), element);
				else {
					OrderList list = (OrderList)element;
					for (int i = 0; i < list.size(); i++) {
						Array.set(result, nextIdx( idx++, size), list.get(i));
					}
				}
			}
			return result;
		}
		
		private int nextIdx(int idx, int size)
		{
			return isDecreasing ? size - 1 - idx : idx; 
		}

		private IBoundNode getContainer() {
			return getChildren()[0];
		}

		public IOpenClass getType() {
			return getContainer().getType();
		}
	}

	public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
			throws Exception {
		BindHelper.processError("This node always binds  with target", node,
				bindingContext);

		return new ErrorBoundNode(node);
	}

	public IBoundNode bindTarget(ISyntaxNode node,
			IBindingContext bindingContext, IBoundNode targetNode)
			throws Exception {

		if (node.getNumberOfChildren() != 1) {
			BindHelper.processError("Index node must have  exactly 1 subnode",
					node, bindingContext);

			return new ErrorBoundNode(node);
		}

		boolean isDecreasing = node.getType().contains("decreasing");
		
		IOpenClass containerType = targetNode.getType();
		IAggregateInfo info = containerType.getAggregateInfo();

		String varName = BindHelper.getTemporaryVarName(bindingContext,
				ISyntaxConstants.THIS_NAMESPACE, TEMPORARY_VAR_NAME);
		ILocalVar var = bindingContext.addVar(ISyntaxConstants.THIS_NAMESPACE,
				varName, info.getComponentType(containerType));

		IBoundNode[] children = bindChildren(node, new TypeBindingContext(
				bindingContext, var));
		IBoundNode orderExpressionNode = checkOrderExpressionBoundNode(
				children[0], bindingContext);

		return new OrderByIndexNode(node, new IBoundNode[] { targetNode,
				orderExpressionNode }, var, isDecreasing);
	}

	static public IBoundNode checkOrderExpressionBoundNode(
			IBoundNode orderExpressionNode, IBindingContext bindingContext) {

		if (orderExpressionNode != null
				&& !Comparable.class.isAssignableFrom(orderExpressionNode
						.getType().getInstanceClass())) {
			BindHelper.processError("Order By expression must be Comparable",
					orderExpressionNode.getSyntaxNode(), bindingContext);
			return new ErrorBoundNode(orderExpressionNode.getSyntaxNode());
		} else {
			return orderExpressionNode;
		}

	}
}
