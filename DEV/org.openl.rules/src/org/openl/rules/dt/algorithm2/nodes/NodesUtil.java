package org.openl.rules.dt.algorithm2.nodes;

import java.util.List;

public class NodesUtil {
	
	static public Object compactSequence(List<Integer> list)
	{
		int size = list.size();
		switch(size)
		{
			case 0:
				throw new Error("0 size");
			case 1:
				return list.get(0);
			case 2:
				return new RuleRange(list.get(0), list.get(1), list.get(1) - list.get(0));
			default:
				
				int first = list.get(0);
				int last = list.get(size - 1);
				int step = (last - first) / (size - 1);
				
				//fast check1 
				if (last != first + step * (size - 1))
					return makeIntArray(list);
				
				//second check
				
				for(int i = 1; i < size - 1; ++i)
				{
					int n = list.get(i);
					if (n != first + i * step)
						return makeIntArray(list);
				}	
				
				return new RuleRange(first, last, step); 
				
		}
	}

	
	
	private static Object makeIntArray(List<Integer> list) {
		int n = list.size();
		int[] ary = new int[n];
		for (int i = 0; i < ary.length; i++) {
			ary[i] = list.get(i);
		}
		return ary;
	}



	static public class RuleRange
	{
		public RuleRange(int ruleN) {
			from = to = ruleN;
		}

		public RuleRange(int from, int to, int step) {
			super();
			this.from = from;
			this.to = to;
			this.step = step;
		}

		public int from, to, step;
	}
	
}
