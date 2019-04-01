package org.openl.rules.calculation.result.convertor2.sample.result;

import java.util.List;

import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.openl.rules.calculation.result.convertor2.RowExtractor;
import org.openl.rules.calculation.result.convertor2.SpreadsheetColumnExtractor;

/**
 * Factory for creating instances of {@link CompoundRowExtractor}
 *
 * @author Marat Kamalov
 *
 */
public class CompoundRowExtractorFactory {

    /**
     * Creates an new instance of {@link CompoundRowExtractor}
     */
    public static RowExtractor<CompoundStep> newInstance(
            List<SpreadsheetColumnExtractor<CompoundStep>> columnExtractors) {
        return new CompoundRowExtractor(columnExtractors);
    }

    /**
     * Extracts compound rows (has other rows inside)
     *
     */
    static class CompoundRowExtractor extends RowExtractor<CompoundStep> {

        CompoundRowExtractor(List<SpreadsheetColumnExtractor<CompoundStep>> columnExtractors) {
            super(columnExtractors);
        }

        @Override
        protected CompoundStep makeRowInstance() {
            return new CompoundStep();
        }

        @Override
        protected CompoundStep afterExtract(CompoundStep step) {
            new CompoundStepPostProcessor().process(step);
            return step;
        }

    }
}
