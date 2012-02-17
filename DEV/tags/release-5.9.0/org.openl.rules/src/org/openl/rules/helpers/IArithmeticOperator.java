package org.openl.rules.helpers;

public interface IArithmeticOperator {

    double accumulate(IDoubleHolder acc, IDoubleHolder param, RoundingOperator rop, double precision);

    String[] getNames();

    String getOperatorSymbol();

    double op(double op1, double op2);
}
