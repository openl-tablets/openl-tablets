package org.apache.poi.hssf.record.formula.functions;

import org.apache.poi.hssf.record.formula.eval.EvaluationException;

/**
 * Calculates the net present value of an investment by using a discount rate
 * and a series of future payments (negative values) and income (positive
 * values). Minimum 2 arguments, first arg is the rate of discount over the
 * length of one period others up to 254 arguments representing the payments and
 * income.
 * 
 * @author SPetrakovsky
 */
public class Npv extends NumericFunction.MultiArg {

	public Npv() {
		super(2, 255);
	}

	@Override
	protected double evaluate(double[] ds) throws EvaluationException {
		double rate = ds[0];
		double sum = 0;
		for (int i = 1; i < ds.length; i++) {
			sum += ds[i] / Math.pow(rate + 1, i);
		}
		return sum;
	}

}
