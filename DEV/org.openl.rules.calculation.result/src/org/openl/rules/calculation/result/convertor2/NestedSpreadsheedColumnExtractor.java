package org.openl.rules.calculation.result.convertor2;

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

/**
 * Column extractor for nesting spreadsheet values(e.g. SpreadsheetResult or
 * SpreadsheetResult[])
 * 
 * @author DLiauchuk
 * 
 */
public class NestedSpreadsheedColumnExtractor extends SpreadsheetColumnExtractor<CompoundStep> {

    /**
     * Indicates the current level of nesting.
     */
    private int nestingLevel;

    /**
     * Configuration for each level of converting.
     */
    private NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> conf;

    public NestedSpreadsheedColumnExtractor(int nestingLevel,
            NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> configuration,
            ColumnToExtract column) {
        super(column);
        this.nestingLevel = nestingLevel;
        this.conf = configuration;
    }

    public NestedSpreadsheedColumnExtractor(int nestingLevel, ColumnToExtract column) {
        this(nestingLevel, null, column);
    }

    /**
     * Gets the configuration
     * 
     * @return {@link NestedSpreadsheetConfiguration}
     */
    public NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> getConfiguration() {
        return conf;
    }

    /**
     * Overrides the parent method. Analyze the 'from' object if it is a
     * {@link SpreadsheetResult} or an array of it. According to this converts
     * and stores the data to object 'to'.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void convertAndStoreData(Object from, CompoundStep to) {
        if (from != null) {
            if (from.getClass().isArray()) {
                // process SpreadsheetResult[] as nesting column value.
                //
                for (SpreadsheetResult result : (SpreadsheetResult[]) from) {
                    CompoundStep compoundStep = conf.initCompoundRowExtractor(null).makeRowInstance();
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

    private <T extends CalculationStep, Q extends CompoundStep> NestedSpreadsheetResultConverter<T, Q> createNextLevelConverter(NestedSpreadsheetConfiguration<T, Q> configuration) {
        return new NestedSpreadsheetResultConverter<T, Q>(nestingLevel + 1, configuration);
    }

    private List<? extends CalculationStep> convertCompoundPremium(SpreadsheetResult result) {
        NestedSpreadsheetResultConverter<? extends CalculationStep, ? extends CompoundStep> converter = createNextLevelConverter(conf);
        return converter.process(result);
    }

}
