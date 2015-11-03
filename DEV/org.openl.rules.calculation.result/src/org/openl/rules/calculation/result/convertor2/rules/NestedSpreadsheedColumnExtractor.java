package org.openl.rules.calculation.result.convertor2.rules;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.List;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;

/**
 * Column extractor for nesting spreadsheet values(e.g. SpreadsheetResult or
 * SpreadsheetResult[])
 * 
 * @author DLiauchuk, Marat Kamalov
 * 
 */
public class NestedSpreadsheedColumnExtractor<T extends CompoundStep> extends SpreadsheetColumnExtractor<T> {

    private RulesConfiguration<?, T> rulesConfiguration;

    public NestedSpreadsheedColumnExtractor(RulesConfiguration<?, T> rulesConfiguration,
            String columnName) {
        super(columnName, rulesConfiguration);
        this.rulesConfiguration = rulesConfiguration;
    }

    /**
     * Gets the configuration
     * 
     * @return {@link RulesConfiguration}
     */
    public RulesConfiguration<?, T> getRulesConfiguration() {
		return rulesConfiguration;
	}

    /**
     * Overrides the parent method. Analyze the 'from' object if it is a
     * {@link SpreadsheetResult} or an array of it. According to this converts
     * and stores the data to object 'to'.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void convertAndStoreData(Object from, T to) {
        if (from != null) {
            if (from.getClass().isArray()) {
                // process SpreadsheetResult[] as nesting column value.
                //
                for (SpreadsheetResult result : (SpreadsheetResult[]) from) {
                    T compoundStep = rulesConfiguration.makeCompoundRowInstance();
                    compoundStep.setSteps((List<CalculationStep>) convertCompoundPremium(result));

                    // additional processing of converted results
                    //
                    postProcess(compoundStep);

                    to.addStep(compoundStep);
                }
            } else {
                // process SpreadsheetResult as nesting column value.
                //
                to.setSteps((List<CalculationStep>) convertCompoundPremium((SpreadsheetResult) from));
            }
            // additional processing of converted results
            //
            postProcess(to);
        }
    }

    /**
     * Override this method for your purpose, if there is a need in additional
     * processing of converted result
     * 
     * @param compoundStep already converted result to {@link CompoundStep}
     */
    protected CompoundStep postProcess(CompoundStep compoundStep) {
        // Default implementation. Do nothing.
        // Override it for you purpose
        return compoundStep;
    }

    private <K extends CalculationStep, Q extends CompoundStep> RulesNestedSpreadsheetResultConverter<K, Q> createNextLevelConverter(RulesConfiguration<K, Q> configuration) {
        return new RulesNestedSpreadsheetResultConverter<K, Q>(configuration);
    }

    private List<? extends CalculationStep> convertCompoundPremium(SpreadsheetResult result) {
    	RulesNestedSpreadsheetResultConverter<? extends CalculationStep, ? extends CompoundStep> converter = createNextLevelConverter(getRulesConfiguration());
        return converter.process(result);
    }

}
