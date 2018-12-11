package org.openl.rules.dt.algorithm2.nodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openl.rules.dt.algorithm.evaluator.RangeIndexedEvaluator;
import org.openl.rules.dt.algorithm2.ConditionDescriptor;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;
import org.openl.rules.dt.algorithm2.NodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class RangeNodeBuilder {

	static public NodeBuilder makeNodeBuilder(ICondition cond, boolean isFirst,
			boolean isLast) {
		RangeIndexedEvaluator rix = (RangeIndexedEvaluator) cond
				.getConditionEvaluator();
		IRangeAdaptor rangeAdaptor = rix.getRangeAdaptor();

		IRangeIndexMap map = makeRangeIndexMap(cond, rangeAdaptor, rix);

		if (isFirst)
			if (isLast)
				return new RangeNodeBuiderVi(cond, isFirst, isLast, map,
						rangeAdaptor);
			else
				return new RangeNodeBuiderMN(cond, isFirst, isLast, map,
						rangeAdaptor);
		else if (isLast)
			return new RangeNodeBuiderVi(cond, isFirst, isLast, map,
					rangeAdaptor);
		else
			return new RangeNodeBuiderMN(cond, isFirst, isLast, map, rangeAdaptor);

	}

	private static IRangeIndexMap makeRangeIndexMap(ICondition cond,
			IRangeAdaptor rangeAdaptor, RangeIndexedEvaluator rix) {

		Class<?> indexType = rangeAdaptor == null ? cond.getEvaluator()
				.getMethod().getType().getInstanceClass() : rangeAdaptor
				.getIndexType();

		Set<Comparable> uniqueRangeBounds = rix.getNparams() == 1 ? makeSingleParamUniqueBounds(
				cond, rix, rangeAdaptor) : makeTwoParamUniqueBounds(cond, rix,
				rangeAdaptor);

		// List<Comparable<?>> list = new
		// ArrayList<Comparable<?>>(uniqueRangeBounds);

		if (indexType == Integer.class)
			return IntRangeIndexMap.makeMap(uniqueRangeBounds);

		if (indexType == Double.class)
			return DoubleRangeIndexMap.makeMap(uniqueRangeBounds);
		
		return ComparableRangeIndexMap.makeMap(uniqueRangeBounds);
		
	}

	private static Set<Comparable> makeSingleParamUniqueBounds(
			ICondition cond, RangeIndexedEvaluator rix,
			IRangeAdaptor rangeAdaptor) {

		Map<Object, Integer> uniqueIndex = cond.getStorageInfo(0)
				.getUniqueIndex();
		Set<Comparable> set = new HashSet<Comparable>();
		for (Object el : uniqueIndex.keySet()) {
			Comparable min = rangeAdaptor.getMin(el);
			Comparable max = rangeAdaptor.getMax(el);
			if (min != null)
				set.add(min);
			if (max != null)
				set.add(max);
		}

		return set;
	}

	private static Set<Comparable> makeTwoParamUniqueBounds(ICondition cond,
			RangeIndexedEvaluator rix, IRangeAdaptor rangeAdaptor) {
		Map<Object, Integer> uniqueIndex0 = cond.getStorageInfo(0)
				.getUniqueIndex();
		Map<Object, Integer> uniqueIndex1 = cond.getStorageInfo(1)
				.getUniqueIndex();
		Set<Comparable> set = new HashSet<Comparable>();
		for (Object el : uniqueIndex0.keySet()) {
			Object min = el;
			if (rangeAdaptor != null)
				min = rangeAdaptor.getMin(min);
			if (min != null)
				set.add((Comparable) min);
		}

		for (Object el : uniqueIndex1.keySet()) {
			Object max = el;
			if (rangeAdaptor != null)
				max = rangeAdaptor.getMax(max);
			if (max != null)
				set.add((Comparable) max);
		}

		return set;
	}

	public static class RangeNodeBuiderVi extends NodeBuilderV {

		private IRangeIndexMap rangeMap;
		private IRangeAdaptor rangeAdaptor;

		public RangeNodeBuiderVi(ICondition condition, boolean isFirst,
				boolean isLast, IRangeIndexMap map, IRangeAdaptor rangeAdaptor) {
			super(condition, isFirst, isLast);
			this.rangeMap = map;
			this.rangeAdaptor = rangeAdaptor;
		}


		protected int calculateNodeIndex(int ruleN) {
			throw new UnsupportedOperationException("calculateNodeIndex");		
			}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor.WithRangeMap(true, condition,
					rangeMap, rangeAdaptor);
		}

		@Override
		protected int getNodesSize() {
			return rangeMap.size();
		}

		@Override
		public boolean isSingleNode(int ruleN) {
			return false;
		}

		@Override
		public Iterator<ISearchTreeNode> findOrCreateNextNodes(
				ISearchTreeNode node, int ruleN) {
			throw new UnsupportedOperationException("findOrCreateNextNodes");		
		}

		
		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			Comparable min = getMin(ruleN);
			Comparable max = getMax(ruleN);

			final int from = min == null ? 0 : rangeMap.findIndex(min);
			final int to = max == null ? rangeMap.size()   : rangeMap.findIndex(max);

			SearchNodeV vnode = (SearchNodeV) node;
	
			boolean res = false;
			
			for (int index = from; index < to; index++) {
				Object oldValue =  vnode.getValue(index);
				
				if (oldValue == null) {
					vnode.setValue(index, ruleN);
					res = true;
				}
				
			}
			

			return res;
		}

		private Comparable getMin(int ruleN) {
			
			Object value = condition.getParamValue(0, ruleN);
			return rangeAdaptor == null ? (Comparable)value : rangeAdaptor.getMin(value);
		}

		private Comparable getMax(int ruleN) {
			
			Object value = condition.getNumberOfParams() == 2 ? condition.getParamValue(1, ruleN) : condition.getParamValue(0, ruleN);
			return rangeAdaptor == null ? (Comparable)value : rangeAdaptor.getMax(value);
		}
		
	}


	public static class RangeNodeBuiderMN extends NodeBuilderN {

		private IRangeIndexMap rangeMap;
		private IRangeAdaptor rangeAdaptor;

		public RangeNodeBuiderMN(ICondition condition, boolean isFirst,
				boolean isLast, IRangeIndexMap map, IRangeAdaptor rangeAdaptor) {
			super(condition, isFirst, isLast);
			this.rangeMap = map;
			this.rangeAdaptor = rangeAdaptor;
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor.WithRangeMap(true, condition,
					rangeMap, rangeAdaptor);
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			throw new UnsupportedOperationException("indexRuleN");
		}

		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			throw new UnsupportedOperationException("findOrCreateNextNode");
		}

		@Override
		public ISearchTreeNode createNode() {
			return new SearchNodeN(new IARTNode[getNodesSize()]);
		}

		@Override
		public boolean isSingleNode(int ruleN) {
			return false;
		}

		@Override
		public Iterator<ISearchTreeNode> findOrCreateNextNodes(
				ISearchTreeNode node, int ruleN) {
			Comparable min = getMin(ruleN);
			Comparable max = getMax(ruleN);

			final int from = min == null ? 0 : rangeMap.findIndex(min);
			final int to = max == null ? rangeMap.size()   : rangeMap.findIndex(max);

			final SearchNodeN snode = (SearchNodeN) node;

			return new Iterator<ISearchTreeNode>() {

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				int current = from;

				@Override
				public boolean hasNext() {
					return current < to;
				}

				@Override
				public ISearchTreeNode next() {
					ISearchTreeNode nextNode = (ISearchTreeNode) snode
							.findNode(current);
					if (nextNode == null) {
						snode.setNode(current, nextNode = next.createNode());
					}

					++current;

					return nextNode;
				}
			};

		}

		private Comparable getMin(int ruleN) {
			
			Object value = condition.getParamValue(0, ruleN);
			return rangeAdaptor == null ? (Comparable)value : rangeAdaptor.getMin(value);
		}

		private Comparable getMax(int ruleN) {
			
			Object value = condition.getNumberOfParams() == 2 ? condition.getParamValue(1, ruleN) : condition.getParamValue(0, ruleN);
			return rangeAdaptor == null ? (Comparable)value : rangeAdaptor.getMax(value);
		}

		@Override
		protected int getNodesSize() {
			return rangeMap.size();
		}

		@Override
		protected int calculateNodeIndex(int ruleN) {
			throw new UnsupportedOperationException("calculateNodeIndex");
		}
	}


	static public interface IRangeIndexMap {
		int findIndex(Object x);

		int size();
	}

	static class IntRangeIndexMap implements IRangeIndexMap {
		int[] ary;

		public IntRangeIndexMap(int[] ary) {
			super();
			this.ary = ary;
		}

		@Override
		public int findIndex(Object x) {
			int idx = Arrays.binarySearch(ary, (Integer) x);
			return idx >= 0 ? idx + 1 : -(idx + 1);
		}

		static IntRangeIndexMap makeMap(Collection c) {
			int[] ary = new int[c.size()];

			Iterator it = c.iterator();
			for (int i = 0; i < ary.length; i++) {
				ary[i] = (Integer) it.next();
			}

			Arrays.sort(ary);

			return new IntRangeIndexMap(ary);
		}

		@Override
		public int size() {
			return ary.length + 1;
		}
	}

	
	static class DoubleRangeIndexMap implements IRangeIndexMap {
		double[] ary;

		public DoubleRangeIndexMap(double[] ary) {
			super();
			this.ary = ary;
		}

		@Override
		public int findIndex(Object x) {
			int idx = Arrays.binarySearch(ary, (Double) x);
			return idx >= 0 ? idx + 1 : -(idx + 1);
		}

		static DoubleRangeIndexMap makeMap(Collection c) {
			double[] ary = new double[c.size()];

			Iterator it = c.iterator();
			for (int i = 0; i < ary.length; i++) {
				ary[i] = (Double) it.next();
			}

			Arrays.sort(ary);

			return new DoubleRangeIndexMap(ary);
		}

		@Override
		public int size() {
			return ary.length + 1;
		}
	}
	
	static class ComparableRangeIndexMap implements IRangeIndexMap {
		Comparable[] ary;

		public ComparableRangeIndexMap(Comparable[] ary) {
			super();
			this.ary = ary;
		}

		@Override
		public int findIndex(Object x) {
			int idx = Arrays.binarySearch(ary, (Comparable) x);
			return idx >= 0 ? idx + 1 : -(idx + 1);
		}

		static ComparableRangeIndexMap makeMap(Collection c) {
			Comparable[] ary = new Comparable[c.size()];

			Iterator it = c.iterator();
			for (int i = 0; i < ary.length; i++) {
				ary[i] = (Comparable) it.next();
			}

			Arrays.sort(ary);

			return new ComparableRangeIndexMap(ary);
		}

		@Override
		public int size() {
			return ary.length + 1;
		}
	}
}
