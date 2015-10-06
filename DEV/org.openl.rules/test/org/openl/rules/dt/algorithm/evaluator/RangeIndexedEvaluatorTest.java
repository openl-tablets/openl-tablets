package org.openl.rules.dt.algorithm.evaluator;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.rules.dt.type.DoubleRangeAdaptor;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.dt.type.IntRangeAdaptor;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;


/**
 * NOTE!!! Fired node doesnt mean that that the same DT rule index will fire.
 * 
 * @author DLiauchuk
 *
 */
@Ignore
public class RangeIndexedEvaluatorTest {
	
	@Test
	public void testDoubleRangeIncluding() {
		
		DoubleRange range1 = new DoubleRange("1-15");
		DoubleRange range2 = new DoubleRange("15.5");
		DoubleRange range3 = new DoubleRange("16.6-17.5");
		
		Object[][] params = new Object[3][];
		params[0] = new Object[]{range1};
		params[1] = new Object[]{range2};
		params[2] = new Object[]{range3};

		ICondition condition = Mockito.mock(ICondition.class);

		IRangeAdaptor adaptor = DoubleRangeAdaptor.getInstance();
		RangeIndexedEvaluator eval = new RangeIndexedEvaluator(adaptor, 1);
		RangeIndex rangeIndex = (RangeIndex)eval.makeIndex(condition, new IntRangeDomain(0, params.length - 1).intIterator());
		
		DecisionTableRuleNode node1 = rangeIndex.findNodeInIndex(Double.valueOf(1));
		assertEquals("First node should fire", 0, rangeIndex.getNodeIndex(node1));
		
		DecisionTableRuleNode node2 = rangeIndex.findNodeInIndex(Double.valueOf(15));
		assertEquals("First node should fire", 0, rangeIndex.getNodeIndex(node2));
		
		DecisionTableRuleNode node3 = rangeIndex.findNodeInIndex(Double.valueOf(0.9));
		assertNull("Not any node match the income value", node3);
		
		DecisionTableRuleNode node4 = rangeIndex.findNodeInIndex(Double.valueOf(15.5));
		assertEquals("Third node should fire", 2, rangeIndex.getNodeIndex(node4));
		
		DecisionTableRuleNode node5 = rangeIndex.findNodeInIndex(Double.valueOf(15.6));
		assertEquals("Fourth node should fire", 3, rangeIndex.getNodeIndex(node5));
		
		DecisionTableRuleNode node6 = rangeIndex.findNodeInIndex(Double.valueOf(16.6));
		assertEquals("Fifth node should fire", 4, rangeIndex.getNodeIndex(node6));
		
		DecisionTableRuleNode node7 = rangeIndex.findNodeInIndex(Double.valueOf(17.5));
		assertEquals("Fifth node should fire", 4, rangeIndex.getNodeIndex(node7));
		
	}
	
	@Test
	public void testIntRangeIncluding() {
		IRangeAdaptor adaptor1 = IntRangeAdaptor.getInstance();
		RangeIndexedEvaluator eval1 = new RangeIndexedEvaluator(adaptor1, 1);
		IntRange range1 = new IntRange("1-12");
		IntRange range2 = new IntRange("16-18");
		IntRange range3 = new IntRange("21-26");
		
		Object[][] params = new Object[3][];
		params[0] = new Object[]{range1};
		params[1] = new Object[]{range2};
		params[2] = new Object[]{range3};

		ICondition condition = Mockito.mock(ICondition.class);

		RangeIndex rangeIndex1 = (RangeIndex)eval1.makeIndex(condition, new IntRangeDomain(0, params.length - 1).intIterator());
		
		DecisionTableRuleNode node0 = rangeIndex1.findNodeInIndex(Integer.valueOf(1));		
		assertEquals("First node should fire", 0, rangeIndex1.getNodeIndex(node0));
		
		DecisionTableRuleNode node1 = rangeIndex1.findNodeInIndex(Integer.valueOf(12));		
		assertEquals("First node should fire", 0, rangeIndex1.getNodeIndex(node1));
		
		DecisionTableRuleNode node2 = rangeIndex1.findNodeInIndex(Integer.valueOf(16));		
		assertEquals("Third node should fire", 2, rangeIndex1.getNodeIndex(node2));
		
		DecisionTableRuleNode node3 = rangeIndex1.findNodeInIndex(Integer.valueOf(17));		
		assertEquals("Third node should fire", 2, rangeIndex1.getNodeIndex(node3));
		
		DecisionTableRuleNode node5 = rangeIndex1.findNodeInIndex(Integer.valueOf(18));
		assertEquals("Third node should fire", 2, rangeIndex1.getNodeIndex(node5));
		
		DecisionTableRuleNode node6 = rangeIndex1.findNodeInIndex(Integer.valueOf(19));
		assertEquals("Fourth should fire", 3, rangeIndex1.getNodeIndex(node6));
		
		DecisionTableRuleNode node7 = rangeIndex1.findNodeInIndex(Integer.valueOf(21));		
		assertEquals("Fifth node should fire", 4, rangeIndex1.getNodeIndex(node7));
		
	}
	
}
