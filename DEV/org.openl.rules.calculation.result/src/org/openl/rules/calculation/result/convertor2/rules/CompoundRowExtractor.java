package org.openl.rules.calculation.result.convertor2.rules;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.util.List;

import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;

public class CompoundRowExtractor<T extends CompoundStep> extends RowExtractor<T> {
	
	private RulesConfiguration<? extends CalculationStep, T> rulesConfiguration;
	
	public CompoundRowExtractor(List<SpreadsheetColumnExtractor<T>> columnExtractors, RulesConfiguration<? extends CalculationStep, T> rulesConfiguration) {
		super(columnExtractors, rulesConfiguration);
		this.rulesConfiguration = rulesConfiguration;
	}

	@Override
	protected T makeRowInstance() {
		return (T) rulesConfiguration.makeCompoundRowInstance();
	}

	@Override
	protected T afterExtract(T step) {
		return (T) rulesConfiguration.afterCompoundStepExtract(step);
	}

}
