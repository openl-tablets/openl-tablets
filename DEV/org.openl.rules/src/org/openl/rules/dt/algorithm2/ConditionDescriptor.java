package org.openl.rules.dt.algorithm2;

import java.util.Map;

import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt.element.ICondition;
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

	
    public boolean calculateCondition(
            int ruleN, SearchContext sc) {

        return condition.calculateCondition(ruleN, sc.target, sc.params, sc.env).getBooleanValue();
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
			Object key = evaluator.invoke(c.target, c.params, c.env);
			if (key == null)
				return null;
			return map.get(key);
		}
		
	}
	
}
