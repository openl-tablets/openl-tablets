package org.openl.rules.calculation.result.convertor2.sample.result;

import java.util.List;

import org.openl.rules.calculation.result.convertor2.RowExtractor;
import org.openl.rules.calculation.result.convertor2.SpreadsheetColumnExtractor;

/**
 * Factory for creating instances of {@link SimpleRowExtractor}
 * 
 * @author tkrivickas
 * 
 */
public class SimpleRowExtractorFactory {

	/**
	 * Creates an new instance of {@link SimpleRowExtractor}
	 */
	public static RowExtractor<SimpleStep> newInstance(List<SpreadsheetColumnExtractor<SimpleStep>> columnExtractors) {
		return new SimpleRowExtractor(columnExtractors);
	}

	/**
	 * Extracts simple (non-compound) rows
	 * 
	 * @author tkrivickas
	 * 
	 */
	static class SimpleRowExtractor extends RowExtractor<SimpleStep> {

		SimpleRowExtractor(List<SpreadsheetColumnExtractor<SimpleStep>> columnExtractors) {
			super(columnExtractors);
		}

		@Override
		protected SimpleStep makeRowInstance() {
			return new SimpleStep();
		}

		@Override
		protected SimpleStep afterExtract(SimpleStep step) {
		    return step;
		}

	}

}