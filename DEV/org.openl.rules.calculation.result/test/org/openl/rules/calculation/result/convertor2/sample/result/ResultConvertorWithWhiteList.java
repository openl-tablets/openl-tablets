package org.openl.rules.calculation.result.convertor2.sample.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.ColumnToExtract;
import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.openl.rules.calculation.result.convertor2.NestedSpreadsheedColumnExtractor;
import org.openl.rules.calculation.result.convertor2.NestedSpreadsheetConfiguration;
import org.openl.rules.calculation.result.convertor2.NestedSpreadsheetResultConverter;
import org.openl.rules.calculation.result.convertor2.RowExtractor;
import org.openl.rules.calculation.result.convertor2.RowFilter;
import org.openl.rules.calculation.result.convertor2.SpreadsheetColumnExtractor;
import org.openl.rules.calculation.result.convertor2.WhiteListRowFilter;

/**
 * Rating result converter to convert {@link SpreadsheetResult} to {@link CompoundStep}.
 * 
 * @author Marat Kamalov
 *
 */
public class ResultConvertorWithWhiteList {

    /**
     * names of the columns that need to be extracted from SpreadsheetResults
     **/
    private static final String CODE_COLUMN = "Code";
    private static final String FORMULA_COLUMN = "Formula";
    private static final String VALUE_COLUMN = "Value";
    private static final String VALUE1_COLUMN = "Value1";
    private static final String TEXT_COLUMN = "Text";

    private NestedSpreadsheetResultConverter<SimpleStep, CompoundStep> converter;

    public ResultConvertorWithWhiteList() {
        init();
    }

    private void init() {
        // columns that should be extracted for all levels
        //
        List<ColumnToExtract> columnsToExtract = new ArrayList<>();
        columnsToExtract.add(new ColumnToExtract(CODE_COLUMN, String.class));
        columnsToExtract.add(new ColumnToExtract(FORMULA_COLUMN, Double.class));
        columnsToExtract.add(new ColumnToExtract(VALUE_COLUMN, Double.class));
        columnsToExtract.add(new ColumnToExtract(TEXT_COLUMN, String.class));

        // add additional column for extraction on vehicle level
        //
        List<ColumnToExtract> columnsOnVehicleLevel = new ArrayList<>(columnsToExtract);
        columnsOnVehicleLevel.add(new ColumnToExtract(VALUE1_COLUMN, SpreadsheetResult.class));

        /**
         * map of columns that should be extracted on each level of convertion
         **/
        Map<Integer, List<ColumnToExtract>> columnsToExtractForLevels = new HashMap<>();
        columnsToExtractForLevels.put(1, columnsToExtract);
        columnsToExtractForLevels.put(2, columnsOnVehicleLevel);
        columnsToExtractForLevels.put(3, columnsToExtract);

        final Set<String> whiteList = new HashSet<>();
        whiteList.add("Row1");

        NestedSpreadsheetConfiguration<SimpleStep, CompoundStep> configuration = new NestedSpreadsheetConfiguration<SimpleStep, CompoundStep>(
            columnsToExtractForLevels) {
            @Override
            protected RowExtractor<CompoundStep> initCompoundRowExtractor(
                    List<SpreadsheetColumnExtractor<CompoundStep>> compoundExtractors) {
                return CompoundRowExtractorFactory.newInstance(compoundExtractors);
            }

            @Override
            protected RowExtractor<SimpleStep> initSimpleRowExtractor(
                    List<SpreadsheetColumnExtractor<SimpleStep>> simpleExtractors) {
                return SimpleRowExtractorFactory.newInstance(simpleExtractors);
            }

            @Override
            protected NestedSpreadsheedColumnExtractor initCompoundColumnExtractor(int nestingLevel,
                    ColumnToExtract column) {
                return new NestedSpreadsheedColumnExtractorWithPostProcessing(nestingLevel, this, column);
            }

            @Override
            protected RowFilter initRowFilter() {
                return WhiteListRowFilter.buildWhiteListRowFilter(whiteList);
            }
        };

        converter = new NestedSpreadsheetResultConverter<>(1, configuration);
    }

    public CompoundStep process(SpreadsheetResult spreadsheetResult) {
        List<CalculationStep> flatResult = converter.process(spreadsheetResult);

        // Bundle into single step
        CompoundStep rootStep = new CompoundStep();
        rootStep.setSteps(flatResult);
        new CompoundStepPostProcessor(0).process(rootStep);
        return rootStep;
    }

}
