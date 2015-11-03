package org.openl.rules.calculation.result.convertor2.rules;

import org.openl.rules.calc.SpreadsheetResult;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.openl.rules.calculation.result.convertor2.CalculationStep;

public interface RulesConfiguration<T extends CalculationStep, Q extends CalculationStep> {

	Integer ColumnPriority(String columnName);
	
	Boolean IsRowReturn(SpreadsheetResult spreadsheetResult, String rowName);

	MappingInfo[] MappingData();

	Q makeCompoundRowInstance();

	Q afterCompoundStepExtract(Q step);

	T makeSimpleRowInstance();

	T afterSimpleStepExtract(T step);

}
