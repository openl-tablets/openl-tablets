package org.openl.rules.dt2.algorithm2;

import java.util.Map;

import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.element.ICondition;
import org.openl.types.IMethodCaller;

public class ConditionDescriptor {

	public ConditionDescriptor(boolean useIndexedValue, ICondition condition) {
		super();
		this.useIndexedValue = useIndexedValue;
		this.condition = condition;
		this.evaluator = condition.getEvaluator();
	}
	public boolean useIndexedValue;
	public ICondition condition;
	public IMethodCaller evaluator;
	
	public Object evaluate(SearchContext c) {
		return evaluator.invoke(c.target, c.params, c.env);
	}

	
	
	static public class WithMap extends ConditionDescriptor
	{
		
		Map<Object, Integer> map;

		public WithMap(boolean useIndexedValue,
				ICondition condition, Map<Object, Integer> map) {
			super(useIndexedValue, condition);
			this.map = map;
		}

		@Override
		public Object evaluate(SearchContext c) {
			return map.get(evaluator.invoke(c.target, c.params, c.env));
		}
		
	}
	
}
