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
import org.openl.rules.calculation.result.convertor2.ConvertationMetadata.NestedType;

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

    public NestedSpreadsheedColumnExtractor(int nestingLevel,
            NestedSpreadsheetConfiguration<? extends CalculationStep, ? extends CompoundStep> configuration,
            ColumnToExtract column) {
        super(column, configuration);
        this.nestingLevel = nestingLevel;
    }

    public NestedSpreadsheedColumnExtractor(int nestingLevel, ColumnToExtract column) {
        this(nestingLevel, null, column);
    }

    /**
     * Overrides the parent method. Analyze the 'from' object if it is a
     * {@link SpreadsheetResult} or an array of it. According to this converts
     * and stores the data to object 'to'.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object convertAndStoreData(Object from, CompoundStep to) {
        NestedType nestedType = null;
        if (from != null) {
            if (from.getClass().isArray()) {
                // process SpreadsheetResult[] as nesting column value.
                //
                for (SpreadsheetResult result : (SpreadsheetResult[]) from) {
                    CompoundStep compoundStep = getConfiguration().initCompoundRowExtractor(null).makeRowInstance();
                    compoundStep.setSteps((List<CalculationStep>) convertCompoundPremium(result));

                    // additional processing of converted results
                    //
                    postProcess(compoundStep);

                    if (getConfiguration().isConvertationMetadataEnabled()) {
                        ConvertationMetadata convertationMetadata = new ConvertationMetadata();
                        compoundStep.setConvertationMetadata(convertationMetadata);
                    }

                    to.addStep(compoundStep);
                }
                nestedType = ConvertationMetadata.NestedType.ARRAY;
            } else {
                // process SpreadsheetResult as nesting column value.
                //
                to.setSteps((List<CalculationStep>) convertCompoundPremium((SpreadsheetResult) from));
                nestedType = ConvertationMetadata.NestedType.SINGLE;
            }
            // additional processing of converted results
            //
            postProcess(to);
        }
        return nestedType;
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

    private <T extends CalculationStep, Q extends CompoundStep> NestedSpreadsheetResultConverter<T, Q> createNextLevelConverter(
            NestedSpreadsheetConfiguration<T, Q> configuration) {
        return new NestedSpreadsheetResultConverter<T, Q>(nestingLevel + 1, configuration);
    }

    private List<? extends CalculationStep> convertCompoundPremium(SpreadsheetResult result) {
        NestedSpreadsheetResultConverter<? extends CalculationStep, ? extends CompoundStep> converter = createNextLevelConverter(
            getConfiguration());
        return converter.process(result);
    }
}
