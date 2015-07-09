package org.openl.rules.dt2.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt2.DTInfo;
import org.openl.rules.dt2.DTScale;
import org.openl.rules.dt2.DecisionTable;


public class IndexInfo {
	
	DecisionTable table;
	
	int fromCondition = 0, 
			toCondition; //defines a range of conditions to be included in the index
	
	int fromRule = 0, 
			toRule;

	private int step = 1;
	
	public int getStep() {
		return step;
	}

	public IndexInfo withTable(DecisionTable t)
	{
		table = t;
		toCondition = table.getConditionRows().length-1;
		toRule = table.getNumberOfRules()-1;
		return this;
	}

	public DecisionTable getTable() {
		return table;
	}

	public int getFromCondition() {
		return fromCondition;
	}

	public int getToCondition() {
		return toCondition;
	}

	public int getFromRule() {
		return fromRule;
	}

	public int getToRule() {
		return toRule;
	}

	public IndexInfo makeVerticalInfo() {
		DTInfo dti = table.getDtInfo();
		return new IndexInfo().withTable(table).withToCondition(dti.getNumberVConditions()-1).withToRule(dti.getScale().getHScale().getMultiplier() - 1);
	}

	
	public IndexInfo makeHorizontalalInfo() {
		DTInfo dti = table.getDtInfo();
		DTScale dts = dti.getScale();
		
		int hSize = dts.getVScale().getMultiplier();
		int vSize = dts.getHScale().getMultiplier();
		
		
		return new IndexInfo().withTable(table).withFromCondition(dti.getNumberVConditions())
				.withToCondition(toCondition).withToRule((hSize-1) * vSize).withStep(vSize);
	}
	
	
	private IndexInfo withStep(int step) {
		this.step  = step;
		return this;
	}

	private IndexInfo withFromCondition(int fromCondition) {
		this.fromCondition = fromCondition; 
		return this;
	}

	private IndexInfo withToRule(int toRule) {
		this.toRule = toRule;
		return this;
	}

	private IndexInfo withToCondition(int toCondition) {
		this.toCondition = toCondition;
		return this;
	}

	public IIntIterator makeRuleIterator()
	{
		return new IntRangeDomain(fromRule, toRule).iterate(step); 
	}
	
	
}
