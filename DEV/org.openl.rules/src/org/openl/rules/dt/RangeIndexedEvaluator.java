/**
 * Created Jul 22, 2007
 */
package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.ADTRuleIndex.DTRuleNode;
import org.openl.rules.dt.ADTRuleIndex.DTRuleNodeBuilder;
import org.openl.util.IntervalMap;
import org.openl.util.OpenIterator;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class RangeIndexedEvaluator  implements IDTConditionEvaluator
{

	IRangeAdaptor adaptor = null;
	
	
	public RangeIndexedEvaluator(IRangeAdaptor adaptor)
	{
		this.adaptor = adaptor;
	}



	public IIntSelector getSelector(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env)
	{
		Object value = condition.getEvaluator().invoke(target, dtparams, env);
		return new RangeSelector(condition, value, target, dtparams, env);
	}

	

	
	static class RangeSelector implements IIntSelector
	{
		IDTCondition condition;
		Object value;
		Object target;
		Object[] dtparams;
		IRuntimeEnv env;
		
		
		public RangeSelector(IDTCondition condition, Object value, Object target, Object[] dtparams, IRuntimeEnv env)
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
			
			return ((Comparable)ruleParams[0]).compareTo(value) <= 0 && ((Comparable)value).compareTo(ruleParams[1]) < 0;
		}
	}

	
	
	
	
	
	
	public ADTRuleIndex makeIndex(Object[][] indexedparams, IIntIterator it)
	{
		
		if (it.size() < 1)
			return null;
		
		IntervalMap map = new IntervalMap();
		DTRuleNodeBuilder emptyBuilder =  new DTRuleNodeBuilder();
		
		for (; it.hasNext(); )
		{
			int i = it.nextInt();
			
			if (indexedparams[i] == null || indexedparams[i][0] == null)
			{
				emptyBuilder.addRule(i);
				
				for (Iterator iter = map.treeMap().values().iterator(); iter.hasNext();)
				{
					ArrayList list = (ArrayList) iter.next();
					list.add(new Integer(i));
				}
				
				continue;
			}	
			
			Comparable vFrom = null;
			Comparable vTo = null;

			if (adaptor == null)
			{	
			
				vFrom = (Comparable)indexedparams[i][0];
				vTo = (Comparable)indexedparams[i][1];
			}
			else
			{
				vFrom = adaptor.getMin(indexedparams[i][0]);
				vTo = adaptor.getMax(indexedparams[i][0]);
			}	
			map.putInterval(vFrom, vTo, new Integer(i));
			
		}
		
		TreeMap tm = map.treeMap();
		
		int n = tm.size();
		
		Comparable[] index = new Comparable[n];
		DTRuleNode[] rules = new DTRuleNode[n];
		
		int i = 0;
		for (Iterator iter = tm.entrySet().iterator(); iter.hasNext();++i)
		{
			Map.Entry element = (Map.Entry) iter.next();
			index[i] = (Comparable)element.getKey();
			
			ArrayList list = (ArrayList)element.getValue();
			int[] idxAry = new int[list.size()];
			for (int j = 0; j < idxAry.length; j++)
			{
				idxAry[j] = ((Integer)list.get(j)).intValue();
			}
			
			rules[i] = new DTRuleNode(idxAry);
		}
		
		
		RangeIndex rix = new RangeIndex(emptyBuilder.makeNode(), index, rules);
		
		
		return rix;
		
	}
	
	
	static class RangeIndex extends ADTRuleIndex
	{


		Comparable[] index;
		DTRuleNode[] rules;
		
		/**
		 * @param emptyOrFormulaNodes
		 */
		public RangeIndex(DTRuleNode emptyOrFormulaNodes, Comparable[] index, DTRuleNode[] rules)
		{
			super(emptyOrFormulaNodes);
			this.index = index;
			this.rules = rules;
		}

		public DTRuleNode findNodeInIndex(Object value)
		{
			int idx = Arrays.binarySearch(index, value);
			if (idx >= 0)
				return rules[idx];
			
			idx = -(idx + 1) - 1;
			return idx < 0 ? null : rules[idx];
			
		}

		public Iterator nodes()
		{
			return OpenIterator.fromArray(rules);
		}
		
	}
	
	

	public boolean isIndexed()
	{
		return true;
	}

}
