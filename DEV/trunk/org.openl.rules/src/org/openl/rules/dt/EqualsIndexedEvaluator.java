/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.ADTRuleIndex.DTRuleNodeBuilder;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class EqualsIndexedEvaluator implements IDTConditionEvaluator
{
	
	
	static class EqualsIndex extends ADTRuleIndex
	{
		
		
		
		HashMap valueNodes = new HashMap();

		public EqualsIndex(DTRuleNode emptyOrFormulaNodes, HashMap valueNodes)
		{
			super(emptyOrFormulaNodes);
			this.valueNodes = valueNodes;
		}
		
		
		public DTRuleNode findNodeInIndex(Object value)
		{
			if (value != null)
			{
				DTRuleNode node = (DTRuleNode)valueNodes.get(value);
				return node;
			}	
			
			return null;
		}


		public Iterator nodes()
		{
			return valueNodes.values().iterator();
		}
	}

	
	public ADTRuleIndex makeIndex(Object[][] indexedparams, IIntIterator it)
	{
		if (it.size() < 1)
			return null;
		
		HashMap map = new HashMap();
		DTRuleNodeBuilder emptyBuilder =  new DTRuleNodeBuilder();

		
		for (; it.hasNext(); )
		{
			int i = it.nextInt();
			
			if (indexedparams[i] == null || indexedparams[i][0] == null)
			{
				emptyBuilder.addRule(i);
				
				for (Iterator iter = map.values().iterator(); iter.hasNext();)
				{
					DTRuleNodeBuilder dtrnb = (DTRuleNodeBuilder) iter.next();
					dtrnb.addRule(i);
				}
				
				continue;
			}	
			
			Object value = indexedparams[i][0];
			DTRuleNodeBuilder dtrb = (DTRuleNodeBuilder)map.get(value);
			if (dtrb == null)
			{	
				dtrb = new DTRuleNodeBuilder(emptyBuilder);
				map.put(value, dtrb);
			}	
			dtrb.addRule(i);
			
			
		}
		

		HashMap nodeMap = new HashMap();
		
		for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
		{
			Map.Entry element = (Map.Entry) iter.next();
			
			nodeMap.put(element.getKey(), ((DTRuleNodeBuilder)element.getValue()).makeNode());
		}
		
		
		EqualsIndex index = new EqualsIndex(emptyBuilder.makeNode(), nodeMap);
		
		
		return index;
		
	}
	
	
	
	
	public IIntSelector getSelector(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env)
	{
		Object value = condition.getEvaluator().invoke(target, dtparams, env);
		return new EqualsSelector(condition, value, target, dtparams, env);
	}

	
	static class EqualsSelector implements IIntSelector
	{
		IDTCondition condition;
		Object value;
		Object target;
		Object[] dtparams;
		IRuntimeEnv env;
		
		
		public EqualsSelector(IDTCondition condition, Object value, Object target, Object[] dtparams, IRuntimeEnv env)
		{
			this.condition = condition;
			this.value = value;
			this.dtparams = dtparams;
			this.env = env;
			this.target = target;
		}


		public boolean select(int rule)
		{
			Object[][] params = condition.getParamValues();
			Object[] ruleParams = params[rule];
			
			if (ruleParams == null)
				return true;
			
			Object[] realParams = new Object[params.length];
			
			FunctionalRow.loadParams(realParams, 0, ruleParams, target, dtparams, env);
			
			if (ruleParams[0] == null)
				return true;
			
			return ruleParams[0].equals(value);
		}
	}








	public boolean isIndexed()
	{
		return true;
	}
}
