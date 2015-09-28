package org.openl.rules.dt.algorithm2.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.dt.algorithm.evaluator.FloatTypeComparator;
import org.openl.rules.dt.algorithm2.ConditionDescriptor;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;
import org.openl.rules.dt.algorithm2.NodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.helpers.NumberUtils;
import org.openl.types.IOpenClass;

public class EqualsNodeBuilder {

	
	static public NodeBuilder makeNodeBuilder(ICondition cond, boolean isFirst, boolean isLast)
	{
		if (isFirst)
			if (isLast)
				return new EqualsNodeBuiderMi(cond, isFirst, isLast);
			else
				return new EqualsNodeBuiderMN(cond, isFirst, isLast);
		else
			if (isLast)
				return new EqualsNodeBuiderVi(cond, isFirst, isLast);
			else
				return new EqualsNodeBuiderN(cond, isFirst, isLast);
			
	}
	
	
	
	public static class EqualsNodeBuiderVi extends NodeBuilderV.SingleV
	{

		private Map<Object, Integer> map;

		public EqualsNodeBuiderVi(ICondition condition, boolean isFirst, boolean isLast)
		{
			super(condition, isFirst, isLast);
			this.map =  initilaizeEqualsIndexMap(getMapKeyType(condition), condition.getStorageInfo(0).getUniqueIndex());
		}
		
		protected int calculateNodeIndex(int ruleN) {
			Object paramValue = condition.getParamValue(0, ruleN);
			int index = map.get(paramValue);    

			return index;
		}
		
		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor.WithMap(true, condition, map);
		}


		@Override
		protected int getNodesSize() {
			return map.size();
		}
		
	}
	
	
	
	public static class EqualsNodeBuiderN extends NodeBuilderN.SingleN {
		
		
		private Map<Object, Integer> map;

		public EqualsNodeBuiderN(ICondition condition, boolean isFirst, boolean isLast)
		{
			super(condition, isFirst, isLast);
			this.map = initilaizeEqualsIndexMap(getMapKeyType(condition), condition.getStorageInfo(0).getUniqueIndex());
		}
		
		protected int calculateNodeIndex(int ruleN) {
			Object paramValue = condition.getParamValue(0, ruleN);
			int index = map.get(paramValue);    

			return index;
		}
		
		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor.WithMap(true, condition, map);
		}


		@Override
		protected int getNodesSize() {
			return map.size();
		}
	}
	
	public static class EqualsNodeBuiderMN extends NodeBuilder.Single
	{

		Map<Object, ISearchTreeNode> map;
		
		public EqualsNodeBuiderMN(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
			map =  initializeEqualsNodeMap( getMapKeyType(condition), condition.getStorageInfo(0).getUniqueIndex().size());
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

			if (nextNode == null)
			{
				nextNode = next.createNode();
				map.put(value, nextNode);
			}	
			
			return nextNode;
		}

		@Override
		public ISearchTreeNode createNode() {
			return new SearchNodeMN(map);
		}
	}
	


	
	
	public static class EqualsNodeBuiderMi extends NodeBuilder.Single
	{

		Map<Object, Integer> map;
		
		public EqualsNodeBuiderMi(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
			map = initilaizeEqualsRuleNMap(getMapKeyType(condition), condition.getStorageInfo(0).getUniqueIndex().size());
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			Object value = condition.getParamValue(0, ruleN);
			if (map.containsKey(value))
				return false;
			
			map.put(value, ruleN);
			
			return true;
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





	public static Map<Object, Integer> initilaizeEqualsIndexMap(IOpenClass type,
			Map<Object, Integer> uniqueIndex) {
		
		if (NumberUtils.isFloatPointType(type.getInstanceClass()))
		{	
			Map<Object, Integer> map = new TreeMap<Object, Integer>(FloatTypeComparator.getInstance());
			map.putAll(uniqueIndex);
			return  map;
		}	
		return uniqueIndex;
	}





	public static Map<Object, Integer> initilaizeEqualsRuleNMap(IOpenClass type,
			int size) {
		if (NumberUtils.isFloatPointType(type.getInstanceClass()))
		{	
			return new TreeMap<Object, Integer>(FloatTypeComparator.getInstance());
		}	
		else return new HashMap<Object, Integer>(size);
	}





	public static IOpenClass getMapKeyType(ICondition condition) {
		return condition.getParameterInfo(0).getParameterDeclaration().getType();
	}





	public static Map<Object, ISearchTreeNode> initializeEqualsNodeMap(
			IOpenClass type, int size) {
		if (NumberUtils.isFloatPointType(type.getInstanceClass()))
		{	
			return new TreeMap<Object, ISearchTreeNode>(FloatTypeComparator.getInstance());
		}	
		else return new HashMap<Object, ISearchTreeNode>(size);
	}
	
	
	
	
}
