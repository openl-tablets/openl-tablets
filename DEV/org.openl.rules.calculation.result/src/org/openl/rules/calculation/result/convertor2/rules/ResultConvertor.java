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


import java.net.URL;
import java.util.List;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.openl.rules.calculation.result.convertor2.SimpleStep;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * Rating result converter to convert {@link SpreadsheetResult} to
 * {@link CompoundStep}.
 * 
 * @author Marat Kamalov
 *
 */
public class ResultConvertor {

	private RulesNestedSpreadsheetResultConverter<SimpleStep, CompoundStep> converter;
	private RulesConfiguration<SimpleStep, CompoundStep> rulesConfiguration;
	private String resourceName;
	private ClassLoader classLoader;
	
	public ResultConvertor(String resourceName) {
		this.resourceName = resourceName;
		init();
	}

	public ResultConvertor(String resourceName, ClassLoader classLoader) {
		this.resourceName = resourceName;
		this.classLoader = classLoader;
		init();
	}

	@SuppressWarnings("unchecked")
	private void init() {
		if (classLoader == null){
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		URL url = classLoader.getResource(resourceName);

		@SuppressWarnings("rawtypes")
		RulesEngineFactory<RulesConfiguration> rulesEngineFactory = new RulesEngineFactory<RulesConfiguration>(url, RulesConfiguration.class);
		rulesConfiguration = (RulesConfiguration<SimpleStep, CompoundStep>) rulesEngineFactory.newEngineInstance();

		converter = new RulesNestedSpreadsheetResultConverter<SimpleStep, CompoundStep>(rulesConfiguration);
	}

	public CompoundStep process(SpreadsheetResult spreadsheetResult) {
		
		List<CalculationStep> flatResult = converter.process(spreadsheetResult);

		// Bundle into single step
		CompoundStep rootStep = new CompoundStep();
		rootStep.setSteps(flatResult);

		rulesConfiguration.afterCompoundStepExtract(rootStep);
		return rootStep;
	}

}
