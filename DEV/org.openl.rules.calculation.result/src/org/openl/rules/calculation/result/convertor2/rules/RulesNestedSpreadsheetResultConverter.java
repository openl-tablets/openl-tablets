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

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.SpreadsheetResultHelper;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The example of flat spreadsheet result structure.
 *                      |---------SimpleRow                     |---------SimpleRow   
 *                      |                                       |   
 *                      |---------CompoundSecondLevelResult-----|---------SimpleRow
 *                      |                                       |
 * UpperLevelResult-----|---------SimpleRow                     |---------CompoundThirdLevelResult1----.....
 *                      |                                       |            
 *                      |---------SimpleRow                     |---------CompoundThirdLevelResult2----.....
 *                      |                                       |    
 *                      |---------SimpleRow                     |---------SimpleRow    
 */

/**
 * SpreadsheetResult convertor that supports nested SpreadsheetResult as column
 * values. Converts the SpreadsheetResult to flat structure.
 *
 * @param <T>
 *            class that will be populated with values, when extracting rows
 *            without compound results.
 * @param
 * 			<Q>
 *            class that will be populated with values, when extracting rows wit
 *            compound results.
 * @author DLiauchuk, Marat Kamalov
 */
public class RulesNestedSpreadsheetResultConverter<T extends CalculationStep, Q extends CompoundStep> {

	private final Logger log = LoggerFactory.getLogger(RulesNestedSpreadsheetResultConverter.class);

	private RulesConfiguration<T, Q> rulesConfiguration;

	/**
	 * @param currentNestingLevel
	 *            the number of the current nesting level
	 * @param configuration
	 *            configuration that is used for extracting rows on this and
	 *            further levels, connat be null. In that case will throw
	 *            {@link IllegalArgumentException}
	 */
	public RulesNestedSpreadsheetResultConverter(RulesConfiguration<T, Q> configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException("Rules configuration cannot be empty");
		}
		this.rulesConfiguration = configuration;
	}

	/**
	 * Converts the spreadsheet result to flat structure.
	 *
	 * @param spreadsheetResult
	 *            {@link SpreadsheetResult} that is going to be converted.
	 * @return converted result, represented in flat structure.
	 */
	public List<CalculationStep> process(SpreadsheetResult spreadsheetResult) {
		List<CalculationStep> steps = new ArrayList<CalculationStep>();
		if (spreadsheetResult != null) {
			int height = spreadsheetResult.getHeight();

			for (int row = 0; row < height; row++) {
				CalculationStep step = processRow(spreadsheetResult, row);
				if (step != null) {
					steps.add(step);
				}
			}
			return steps;
		}
		if (log.isWarnEnabled()) {
			log.warn("Spreadsheet result is null");
		}
		return steps;
	}

	@SuppressWarnings("unchecked")
	private CalculationStep processRow(SpreadsheetResult spreadsheetResult, int row) {
		T step = null;
		List<SpreadsheetColumnExtractor<Q>> extractors = new ArrayList<SpreadsheetColumnExtractor<Q>>();

		boolean isNestedRow = false;
		int minNestedPriority = -1;
		for (String columnName : spreadsheetResult.getColumnNames()) {
			int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(columnName,
					spreadsheetResult.getColumnNames());
			Object valueInColumn = spreadsheetResult.getValue(row, columnIndex);
			if (valueIsNested(valueInColumn)) {
				int columnPriority = rulesConfiguration.ColumnPriority(columnName);
				if ((columnPriority <= minNestedPriority || minNestedPriority == -1)) {
					minNestedPriority = columnPriority;
				}
			}
		}

		for (String columnName : spreadsheetResult.getColumnNames()) {
			int columnIndex = SpreadsheetResultHelper.getColumnIndexByName(columnName,
					spreadsheetResult.getColumnNames());
			Object valueInColumn = spreadsheetResult.getValue(row, columnIndex);
			int columnPriority = rulesConfiguration.ColumnPriority(columnName);
			if (!isNestedRow && minNestedPriority > 0 && minNestedPriority == columnPriority
					&& valueIsNested(valueInColumn)) {
				extractors.add(new NestedSpreadsheedColumnExtractor<Q>(rulesConfiguration, columnName));
				isNestedRow = true;
			} else {
				extractors.add(new SpreadsheetColumnExtractor<Q>(columnName, rulesConfiguration));
			}
		}

		if (isNestedRow) {
			CompoundRowExtractor<Q> rowExtractor = new CompoundRowExtractor<Q>(extractors, rulesConfiguration);
			step = (T) rowExtractor.extract(spreadsheetResult, row);
		} else {
			List<SpreadsheetColumnExtractor<T>> simpleExtractors = new ArrayList<SpreadsheetColumnExtractor<T>>();
			for (String columnName : spreadsheetResult.getColumnNames()) {
				simpleExtractors.add(new SpreadsheetColumnExtractor<T>(columnName, rulesConfiguration));
			}
			SimpleRowExtractor<T> rowExtractor = new SimpleRowExtractor<T>(simpleExtractors, rulesConfiguration);
			step = (T) rowExtractor.extract(spreadsheetResult, row);
		}

		return step;
	}

	private static boolean valueIsNested(Object value) {
		if ((value instanceof SpreadsheetResult) || (value instanceof SpreadsheetResult[])) {
			return true;
		}
		return false;
	}

}
