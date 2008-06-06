package org.openl.rules.validator.dt;

import org.openl.rules.helpers.IntRange;

import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntExp;

public class CtrIntRange extends IntRange 
{

	public CtrIntRange(String s) {
		super(s);
	}
	
	
	
	public CtrIntRange(int min, int max) {
		super(min, max);
	}



	public IntBoolExp contains(IntExp exp)
	{
		return exp.ge(getMin()).and(exp.le(getMax()));
	}

}
