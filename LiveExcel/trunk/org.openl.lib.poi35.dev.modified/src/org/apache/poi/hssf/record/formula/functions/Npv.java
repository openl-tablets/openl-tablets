package org.apache.poi.hssf.record.formula.functions;

import org.apache.poi.hssf.record.formula.eval.EvaluationException;

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
