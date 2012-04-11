package org.openl.rules.helpers;

public 	interface IArithmeticOperator
{
	public double op(double op1, double op2);
	public double accumulate(IDoubleHolder acc, IDoubleHolder param, RoundingOperator rop, double precision);
	public String[] getNames();
	public String getOperatorSymbol();
}

