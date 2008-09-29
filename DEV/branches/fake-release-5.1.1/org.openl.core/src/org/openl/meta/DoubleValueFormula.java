package org.openl.meta;

import java.util.Iterator;

import org.openl.util.OpenIterator;


public class DoubleValueFormula extends DoubleValue
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3215205953478317387L;

	public DoubleValueFormula(DoubleValue dv1, DoubleValue dv2, double value, String operand, boolean isMultiplicative)
	{
		this.dv1 = dv1;
		this.dv2 = dv2;
		this.value = value;
		this.operand = operand;
		this.isMultiplicative = isMultiplicative;
	}
	
	String operand;
	DoubleValue dv1, dv2;
	boolean isMultiplicative;

	protected String printContent(int mode, boolean fromMultiplicativeExpr, boolean inBrackets)
	{
		if ((mode & EXPAND_FORMULA) == 0)
			return super.printContent(mode, fromMultiplicativeExpr, inBrackets);
		
		StringBuffer res = new StringBuffer();
		if (!inBrackets)
			if (!isMultiplicative && fromMultiplicativeExpr || ((mode & PRINT_VALUE_IN_EXPANDED) != 0) && isMultiplicative && !fromMultiplicativeExpr)
				res.append('(');

		if (((mode & PRINT_VALUE_IN_EXPANDED) != 0) && isMultiplicative && !fromMultiplicativeExpr)
			res.append(printValue()+"=");
		
		
		res.append(dv1.printExplanationLocal(mode, isMultiplicative));
		res.append(operand);
		res.append(dv2.printExplanationLocal(mode, isMultiplicative));

		if (!inBrackets)
			if (!isMultiplicative && fromMultiplicativeExpr || ((mode & PRINT_VALUE_IN_EXPANDED) != 0) && isMultiplicative && !fromMultiplicativeExpr)
				res.append(')');
		return res.toString();
	}

	public String getDisplayName(int mode)
	{
		switch(mode)
		{
			case SHORT: 
				return super.getDisplayName(mode);
			default:	
				return super.getDisplayName(mode) + " = " +  dv1.getDisplayName(mode-1) + operand + dv2.getDisplayName(mode-1);
		}
	}

	
	public DoubleValue[] getArguments()
	{
		return new DoubleValue[]{dv1, dv2};
	}

	public DoubleValue getDv1()
	{
		return dv1;
	}

	public void setDv1(DoubleValue dv1)
	{
		this.dv1 = dv1;
	}

	public DoubleValue getDv2()
	{
		return dv2;
	}

	public void setDv2(DoubleValue dv2)
	{
		this.dv2 = dv2;
	}

	public boolean isMultiplicative()
	{
		return isMultiplicative;
	}

	public void setMultiplicative(boolean isMultiplicative)
	{
		this.isMultiplicative = isMultiplicative;
	}

	public String getOperand()
	{
		return operand;
	}

	public void setOperand(String operand)
	{
		this.operand = operand;
	}

	public Iterator getChildren()
	{
		return OpenIterator.fromArray(new DoubleValue[]{dv1, dv2});
	}

	public String getType()
	{
		return "formula."+operand;
	}

	public boolean isLeaf()
	{
		return false;
	}
	
	
	
	

}
