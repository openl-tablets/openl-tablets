package org.openl.rules.calculation.result.convertor2.sample.result;

import org.openl.rules.calculation.result.convertor2.ColumnToExtract;
import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.openl.rules.calculation.result.convertor2.NestedSpreadsheedColumnExtractor;
import org.openl.rules.calculation.result.convertor2.NestedSpreadsheetConfiguration;

/**
 * Injects implementation of post processing of converted results to
 * {@link NestedSpreadsheedColumnExtractor}
 * 
 * @author tkrivickas, Marat Kamalov
 * 
 */
public class NestedSpreadsheedColumnExtractorWithPostProcessing extends NestedSpreadsheedColumnExtractor {

    private CompoundStepPostProcessor compoundStepPostProcessor = new CompoundStepPostProcessor();

    public CompoundStepPostProcessor getCompoundStepPostProcessor() {
        return compoundStepPostProcessor;
    }

    public NestedSpreadsheedColumnExtractorWithPostProcessing(int nestingLevel,
            NestedSpreadsheetConfiguration<?, ?> configuration, ColumnToExtract column) {
        super(nestingLevel, configuration, column);
    }

    public NestedSpreadsheedColumnExtractorWithPostProcessing(int nestingLevel, ColumnToExtract column) {
        super(nestingLevel, column);
    }

    @Override
    protected CompoundStep postProcess(CompoundStep compoundStep) {
        getCompoundStepPostProcessor().process((CompoundStep) compoundStep);
        return compoundStep;
    }

}
