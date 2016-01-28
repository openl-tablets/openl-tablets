package org.openl.rules.dt.index;

import java.util.Arrays;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeIndex extends ARuleIndex {

	protected Comparable<?>[] index;
	protected DecisionTableRuleNode[] rules;

	protected IRangeAdaptor<?, ?> adaptor;

	public RangeIndex(DecisionTableRuleNode emptyOrFormulaNodes,
			Comparable<?>[] index, DecisionTableRuleNode[] rules,
			IRangeAdaptor<?, ?> adaptor) {
		super(emptyOrFormulaNodes);

		this.index = index;
		this.rules = rules;
		this.adaptor = adaptor;
	}

	@Override
	public DecisionTableRuleNode findNodeInIndex(Object value) {

		int idx = Arrays.binarySearch(index, convertValueForSearch(value));

		if (idx >= 0) {
			return rules[idx];
		}

		int insertionPoint = -(idx + 1);

		if (insertionPoint < index.length && insertionPoint > 0) {
			return rules[insertionPoint - 1];
		}

		return null;
		// return idx < 0 ? null : rules[idx];
	}

	/**
	 * Converts value for binary search in index(Because different subclasses of
	 * {@link Number} are not comparable).
	 *
	 * @param value
	 *            Value to convert
	 * @return New value that is adapted for binary search.
	 */
	protected Object convertValueForSearch(Object value) {
		if (index.length < 1) {
			return value; // there is no values in index to compare => no reason
			// to convert
		}

		if (adaptor != null) {
			return adaptor.adaptValueType(value);
		}
		return value;
	}

	@Override
	public Iterable<DecisionTableRuleNode> nodes() {
		return Arrays.asList(rules);
	}

	/**
	 * Used for tests
	 *
	 * @param node
	 * @return
	 */
	public int getNodeIndex(DecisionTableRuleNode node) {
		for (int i = 0; i < rules.length; i++) {
			// check the node by the link
			//
			if (rules[i] == node) {
				return i;
			}
		}
		return -1;
	}

}