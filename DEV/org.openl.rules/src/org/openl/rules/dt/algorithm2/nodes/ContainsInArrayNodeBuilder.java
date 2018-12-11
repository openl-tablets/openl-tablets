package org.openl.rules.dt.algorithm2.nodes;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.dt.algorithm.evaluator.FloatTypeComparator;
import org.openl.rules.dt.algorithm2.ConditionDescriptor;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;
import org.openl.rules.dt.algorithm2.NodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.helpers.NumberUtils;
import org.openl.types.IOpenClass;

public class ContainsInArrayNodeBuilder {

	static public NodeBuilder makeNodeBuilder(ICondition cond, boolean isFirst,
			boolean isLast) {
		if (isFirst)
			if (isLast)
				return new EqualsNodeBuiderMi(cond, isFirst, isLast);
			else
				return new EqualsNodeBuiderMN(cond, isFirst, isLast);
		else if (isLast)
			return new EqualsNodeBuiderVi(cond, isFirst, isLast);
		else
			return new EqualsNodeBuiderN(cond, isFirst, isLast);

	}

	public static class EqualsNodeBuiderVi extends NodeBuilderV.SingleV {

		private Map<Object, Integer> map;

		public EqualsNodeBuiderVi(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
			this.map = initilaizeEqualsIndexMap(getMapKeyType(condition),
					condition.getStorageInfo(0).getUniqueIndex());
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor.WithMap(true, condition, map);
		}

		@Override
		protected int getNodesSize() {
			return map.size();
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {

			SearchNodeV vnode = (SearchNodeV) node;

			Object ary = condition.getParamValue(0, ruleN);

			int len = Array.getLength(ary);
			boolean ret = false;
			for (int i = 0; i < len; i++) {
				Object value = Array.get(ary, i);
				int index = map.get(value);
				Object oldValue = vnode.getValue(index);

				if (oldValue == null) {
					vnode.setValue(index, ruleN);
					ret = true;
				}

			}
			return ret;
		}

		@Override
		protected int calculateNodeIndex(int ruleN) {
			throw new UnsupportedOperationException();
		}

	}

	public static class EqualsNodeBuiderN extends NodeBuilderN {

		private Map<Object, Integer> map;

		public EqualsNodeBuiderN(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
			this.map = initilaizeEqualsIndexMap(getMapKeyType(condition),
					condition.getStorageInfo(0).getUniqueIndex());
		}

		protected int calculateNodeIndex(int ruleN) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor.WithMap(true, condition, map);
		}

		@Override
		protected int getNodesSize() {
			return map.size();
		}

		@Override
		public boolean isSingleNode(int ruleN) {
			return false;
		}

		@Override
		public Iterator<ISearchTreeNode> findOrCreateNextNodes(
				ISearchTreeNode node, int ruleN) {
			final SearchNodeN snode = (SearchNodeN) node;
			final Object ary = condition.getParamValue(0, ruleN);
			final int len = Array.getLength(ary);

			return new Iterator<ISearchTreeNode>() {

				int current = 0;

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean hasNext() {
					return current < len;
				}

				@Override
				public ISearchTreeNode next() {
					Object nextValue = Array.get(ary, current++);
					int index = map.get(nextValue);
					IARTNode nextNode = snode.findNode(index);
					if (nextNode == null) {
						snode.setNode(index, nextNode = next.createNode());
					}

					return (ISearchTreeNode) nextNode;
				}
			};

		}
	}

	public static class EqualsNodeBuiderMN extends NodeBuilder {

		Map<Object, ISearchTreeNode> map;

		public EqualsNodeBuiderMN(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
			map = initializeEqualsNodeMap(getMapKeyType(condition), condition
					.getStorageInfo(0).getUniqueIndex().size());
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor(true, condition);
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			throw new UnsupportedOperationException("indexRuleN");
		}

		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			Object value = condition.getParamValue(0, ruleN);

			ISearchTreeNode nextNode = map.get(value);

			if (nextNode == null) {
				nextNode = next.createNode();
				map.put(value, nextNode);
			}

			return nextNode;
		}

		@Override
		public ISearchTreeNode createNode() {
			return new SearchNodeMN(map);
		}

		@Override
		public boolean isSingleNode(int ruleN) {
			return false;
		}

		@Override
		public Iterator<ISearchTreeNode> findOrCreateNextNodes(
				ISearchTreeNode node, int ruleN) {
			final Object ary = condition.getParamValue(0, ruleN);
			final int len = Array.getLength(ary);

			return new Iterator<ISearchTreeNode>() {

				int current = 0;


				@Override
				public boolean hasNext() {
					return current < len;
				}

				@Override
				public ISearchTreeNode next() {
					Object nextValue = Array.get(ary, current++);
					ISearchTreeNode nextNode = map.get(nextValue);
					if (nextNode == null) {
						nextNode = next.createNode();
						map.put(nextValue, nextNode);
					}

					return (ISearchTreeNode) nextNode;
				}
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	public static class EqualsNodeBuiderMi extends NodeBuilder.Single {

		Map<Object, Integer> map;

		public EqualsNodeBuiderMi(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
			map = initilaizeEqualsRuleNMap(getMapKeyType(condition), condition
					.getStorageInfo(0).getUniqueIndex().size());
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			Object ary = condition.getParamValue(0, ruleN);
			int len = Array.getLength(ary);

			boolean ret = false;
			for (int i = 0; i < len; i++) {
				Object value = Array.get(ary, i);
				if (map.containsKey(value))
					continue;
				;

				map.put(value, ruleN);

				ret = true;
			}

			return ret;
		}

		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			throw new UnsupportedOperationException("findOrCreateNextNode");
		}

		@Override
		public ISearchTreeNode createNode() {
			return new SearchNodeMi(map);
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor(true, condition);
		}

	}

	public static Map<Object, Integer> initilaizeEqualsIndexMap(
			IOpenClass type, Map<Object, Integer> uniqueIndex) {
		Map<Object, Integer> map = null;
		if (NumberUtils.isFloatPointType(type.getInstanceClass()
				.getComponentType())) {
			map = new TreeMap<Object, Integer>(
					FloatTypeComparator.getInstance());
		} else
			map = new HashMap<Object, Integer>(uniqueIndex.size());

		int index = 0;
		for (Iterator<Map.Entry<Object, Integer>> iterator = uniqueIndex
				.entrySet().iterator(); iterator.hasNext();) {
			Object ary = iterator.next().getKey();
			int len = Array.getLength(ary);
			for (int i = 0; i < len; i++) {
				Object key = Array.get(ary, i);
				if (map.containsKey(key))
					continue;
				map.put(key, index++);
			}
		}

		return map;
	}

	public static Map<Object, Integer> initilaizeEqualsRuleNMap(
			IOpenClass type, int size) {
		if (NumberUtils.isFloatPointType(type.getInstanceClass()
				.getComponentType())) {
			return new TreeMap<Object, Integer>(
					FloatTypeComparator.getInstance());
		} else
			return new HashMap<Object, Integer>(size);
	}

	public static IOpenClass getMapKeyType(ICondition condition) {
		return condition.getParameterInfo(0).getParameterDeclaration()
				.getType();
	}

	public static Map<Object, ISearchTreeNode> initializeEqualsNodeMap(
			IOpenClass type, int size) {
		if (NumberUtils.isFloatPointType(type.getInstanceClass()
				.getComponentType())) {
			return new TreeMap<Object, ISearchTreeNode>(
					FloatTypeComparator.getInstance());
		} else
			return new HashMap<Object, ISearchTreeNode>(size);
	}

}
