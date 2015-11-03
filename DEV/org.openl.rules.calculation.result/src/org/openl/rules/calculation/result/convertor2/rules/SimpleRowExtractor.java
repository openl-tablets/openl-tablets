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

public class SimpleRowExtractor<T extends CalculationStep> extends RowExtractor<T> {

	private RulesConfiguration<T, ? extends CompoundStep> rulesConfiguration;

	public SimpleRowExtractor(List<SpreadsheetColumnExtractor<T>> columnExtractors, RulesConfiguration<T, ? extends CompoundStep> rulesConfiguration) {
		super(columnExtractors, rulesConfiguration);
		this.rulesConfiguration  = rulesConfiguration;
	}

	@Override
	protected T makeRowInstance() {
		return rulesConfiguration.makeSimpleRowInstance();
	}

	@Override
	protected T afterExtract(T step) {
		return rulesConfiguration.afterSimpleStepExtract(step);
	}

}
