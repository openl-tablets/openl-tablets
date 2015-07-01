package org.openl.rules.dt2.algorithm2;

import java.util.Iterator;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt2.DecisionTable;
import org.openl.rules.dt2.algorithm.DecisionTableAlgorithmBuilder;
import org.openl.rules.dt2.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dt2.algorithm.IndexInfo;
import org.openl.rules.dt2.algorithm.evaluator.EqualsIndexedEvaluator;
import org.openl.rules.dt2.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt2.algorithm2.nodes.EqualsNodeBuilder;
import org.openl.rules.dt2.element.ICondition;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenMethodHeader;

public class DecisionTableAlgorithmBuilder2 extends
		DecisionTableAlgorithmBuilder {

	public DecisionTableAlgorithmBuilder2(DecisionTable decisionTable,
			IOpenMethodHeader header, OpenL openl, ComponentOpenClass module,
			IBindingContextDelegator bindingContextDelegator) {
		super(decisionTable, header, openl, module, bindingContextDelegator);
	}
	
	
	

	protected DecisionTableSearchTree buildSearchTree(IndexInfo info)
			throws SyntaxNodeException {

		int first = info.getFromCondition();
		int last = info.getToCondition();
		IBaseCondition[] cc = table.getConditionRows();

		if (cc.length <= first || first > last)
			return new DecisionTableSearchTree(null, null, info);

		
		
		nodeBuilders = makeNodeBuilders(first, last);

		ISearchTreeNode root = nodeBuilders[0].createNode();

		for (IIntIterator ruleIterator = info.makeRuleIterator(); ruleIterator
				.hasNext();) {
			int ruleN = ruleIterator.nextInt();
			indexRule(ruleN, info, 0, root);
		}
		
		root = (ISearchTreeNode) root.compact();

		ConditionDescriptor[] descriptors = new ConditionDescriptor[nodeBuilders.length];
		for (int i = 0; i < descriptors.length; i++) {
			descriptors[i] = nodeBuilders[i].makeDescriptor();
		}
		
		DecisionTableSearchTree searchTree = new DecisionTableSearchTree(root, descriptors, info);
		return searchTree;
	}

	private NodeBuilder[] makeNodeBuilders(int first, int last) {
		int len = last- first + 1;
		NodeBuilder[] nb = new NodeBuilder[len];
		
		
		for (int i = 0; i < len; i++) {
			int idx = i + first;
			ICondition cond = table.getCondition(idx);
			boolean isFirst = idx == first;
			boolean isLast = idx == last;
			
			nb[i] = makeNodeBuilder(isFirst, isLast, cond);
			if (!isFirst)
				nb[i - 1].next = nb[i];
			
		}
		return nb;
	}

	private NodeBuilder makeNodeBuilder(boolean isFirst, boolean isLast,
			ICondition cond) {
		
		IConditionEvaluator ce = cond.getConditionEvaluator();
		if (ce instanceof EqualsIndexedEvaluator)
		{
			return EqualsNodeBuilder.makeNodeBuilder(cond, isFirst, isLast);
		}	
		
		throw new UnsupportedOperationException("Evaluator: " + ce.getClass().getName());
	}

	NodeBuilder[] nodeBuilders;

	private boolean indexRule(int ruleN, IndexInfo info, int nbIndex,
			ISearchTreeNode node) {

		NodeBuilder nb = nodeBuilders[nbIndex];
		if (nb.isLast)
			return nb.indexRuleN(node, ruleN);
		else {
			if (nb.isSingleNode(ruleN)) {
				ISearchTreeNode nextNode = nb.findOrCreateNextNode(node, ruleN);
				return indexRule(ruleN, info, nbIndex + 1, nextNode);
			} else {
				boolean res = false;
				Iterator<ISearchTreeNode> it = nb.findOrCreateNextNodes(node, ruleN);
				for (; it.hasNext();) {
					ISearchTreeNode nextNode =  it.next();
					res |= indexRule(ruleN, info, nbIndex + 1, nextNode); 
				}
				return res;
			}

		}

	}

	
	protected IDecisionTableAlgorithm makeHorizontalAlgorithm()
			throws SyntaxNodeException {

		IndexInfo hInfo = baseInfo.makeHorizontalalInfo();

		IDecisionTableAlgorithm alg = buildSearchTree(hInfo);

		return alg;
	}

	protected IDecisionTableAlgorithm makeFullAlgorithm()
			throws SyntaxNodeException {
		IDecisionTableAlgorithm alg = buildSearchTree(baseInfo);

		return alg;
	}

	protected IDecisionTableAlgorithm makeVerticalAlgorithm()
			throws SyntaxNodeException {

		IndexInfo vInfo = baseInfo.makeVerticalInfo();

		IDecisionTableAlgorithm alg = buildSearchTree(vInfo);

		return alg;
	}
	
	
}
