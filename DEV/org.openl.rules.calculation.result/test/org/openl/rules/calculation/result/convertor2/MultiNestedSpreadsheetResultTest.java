package org.openl.rules.calculation.result.convertor2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.openl.rules.calc.SpreadsheetResult;

public class MultiNestedSpreadsheetResultTest {

    private static final String RES1_COLUMN = "Result_1";

    private static final String RES_COLUMN = "Result";

    private static final String CODE_COLUMN = "Code";

    public static NestedSpreadsheetConfiguration<CodeStep, CompoundStep> conf;
    static {
        ColumnToExtract c1 = new ColumnToExtract(CODE_COLUMN, String.class);
        ColumnToExtract c2 = new ColumnToExtract(RES_COLUMN);
        ColumnToExtract c3 = new ColumnToExtract(RES1_COLUMN);

        Map<Integer, List<ColumnToExtract>> columnsToExtract = new HashMap<>();

        List<ColumnToExtract> firstLevel = new ArrayList<>();
        firstLevel.add(c1);
        firstLevel.add(c2);
        firstLevel.add(c3);

        List<ColumnToExtract> secondLevel = new ArrayList<>();
        secondLevel.add(c1);
        secondLevel.add(c2);

        columnsToExtract.put(1, firstLevel);
        columnsToExtract.put(2, secondLevel);

        conf = new NestedSpreadsheetConfiguration<CodeStep, CompoundStep>(columnsToExtract) {

            @Override
            protected RowExtractor<CodeStep> initSimpleRowExtractor(List<SpreadsheetColumnExtractor<CodeStep>> simpleExtractors) {

                return new RowExtractor<CodeStep>(simpleExtractors) {

                    @Override
                    protected SimpleStep makeRowInstance() {

                        return new SimpleStep();
                    }

                    @Override
                    protected CodeStep afterExtract(CodeStep step) {
                        // Do nothing
                        return step;
                    }
                };
            }

            @Override
            protected RowExtractor<CompoundStep> initCompoundRowExtractor(List<SpreadsheetColumnExtractor<CompoundStep>> compoundExtractors) {

                return new RowExtractor<CompoundStep>(compoundExtractors) {

                    @Override
                    protected CompoundStep makeRowInstance() {
                        return new CompoundStep();
                    }

                    @Override
                    protected CompoundStep afterExtract(CompoundStep step) {
                        // Do nothing                        
                        return step;
                    }
                };
            }
        };
    }

    @Test
    public void test() {
        SpreadsheetResult res = getMockSpreadsheetResult();

        NestedSpreadsheetResultConverter<CodeStep, CompoundStep> conv = new NestedSpreadsheetResultConverter<>(1,
            conf);
        List<CalculationStep> result = conv.process(res);

        assertEquals("firstNested", ((CodeStep) result.get(0)).getCode());
        assertEquals(CompoundStep.class, result.get(0).getClass());
        // step is simple because doesn`t contain columns for extraction
        //
        assertEquals(SimpleStep.class, ((CompoundStep) result.get(0)).getSteps().get(0).getClass());
        assertEquals("nestedColumn1", ((SimpleStep) ((CompoundStep) result.get(0)).getSteps().get(0)).getCode());

        assertEquals("secondNested", ((CodeStep) result.get(1)).getCode());
        assertEquals(CompoundStep.class, result.get(1).getClass());
        // step is compound because contains column for extraction
        //
        assertEquals(SimpleStep.class, ((CompoundStep) result.get(1)).getSteps().get(0).getClass());
        assertEquals("nestedColumn2", ((SimpleStep) ((CompoundStep) result.get(1)).getSteps().get(0)).getCode());
    }

    @Test
    public void testArraySpr() {
        SpreadsheetResult upperSpr = getMockArraySpreadsheetResult();
        NestedSpreadsheetResultConverter<CodeStep, CompoundStep> converter = new NestedSpreadsheetResultConverter<>(1,
            conf);
        List<CalculationStep> result = converter.process(upperSpr);

        assertNotNull(result);
        assertEquals("firstNested", ((CodeStep) result.get(0)).getCode());
        assertEquals(2, ((CompoundStep) result.get(1)).getSteps().size());
        assertEquals("nestedColumn2_1",
            ((SimpleStep) ((CompoundStep) ((CompoundStep) result.get(1)).getSteps().get(0)).getSteps().get(0)).getCode());
        assertEquals("nestedColumn2_2",
            ((SimpleStep) ((CompoundStep) ((CompoundStep) result.get(1)).getSteps().get(1)).getSteps().get(0)).getCode());
    }

    private SpreadsheetResult getMockSpreadsheetResult() {
        // create upper level spreadsheet
        //
        SpreadsheetResult upperSpr = getSpreadsheet(new String[] { CODE_COLUMN, RES_COLUMN, RES1_COLUMN }, 4, 5);

        // create first nested spreadsheet
        //
        SpreadsheetResult nested = getSpreadsheet(new String[] { CODE_COLUMN, RES_COLUMN }, 1, 1);
        Mockito.when(nested.getValue(0, 0)).thenReturn("nestedColumn1");

        // create second nested spreadsheet
        //
        SpreadsheetResult nested1 = getSpreadsheet(new String[] { CODE_COLUMN, RES_COLUMN }, 1, 1);
        Mockito.when(nested1.getValue(0, 0)).thenReturn("nestedColumn2");

        // put the first nested to the 2nd column in the first row
        //
        Mockito.when(upperSpr.getValue(0, 0)).thenReturn("firstNested");
        Mockito.when(upperSpr.getValue(0, 1)).thenReturn(nested);

        // put the second nested to the 3rd column in the second row
        //
        Mockito.when(upperSpr.getValue(1, 0)).thenReturn("secondNested");
        Mockito.when(upperSpr.getValue(1, 2)).thenReturn(nested1);
        return upperSpr;
    }

    private SpreadsheetResult getMockArraySpreadsheetResult() {
        // create upper level spreadsheet
        //
        SpreadsheetResult upperSpr = getSpreadsheet(new String[] { CODE_COLUMN, RES_COLUMN, RES1_COLUMN }, 4, 5);

        // create first nested spreadsheet
        //
        SpreadsheetResult nested = getSpreadsheet(new String[] { CODE_COLUMN, RES_COLUMN }, 1, 1);
        Mockito.when(nested.getValue(0, 0)).thenReturn("nestedColumn1");

        SpreadsheetResult nested2 = getSpreadsheet(new String[] { CODE_COLUMN, RES_COLUMN }, 1, 1);
        Mockito.when(nested2.getValue(0, 0)).thenReturn("nestedColumn2_1");

        SpreadsheetResult nested2_1 = getSpreadsheet(new String[] { CODE_COLUMN, RES_COLUMN }, 1, 1);
        Mockito.when(nested2_1.getValue(0, 0)).thenReturn("nestedColumn2_2");

        // init spreadsheet array result
        //
        SpreadsheetResult[] array = new SpreadsheetResult[2];
        array[0] = nested2;
        array[1] = nested2_1;

        // put the first nested to the 2nd column in the first row
        //
        Mockito.when(upperSpr.getValue(0, 0)).thenReturn("firstNested");
        Mockito.when(upperSpr.getValue(0, 1)).thenReturn(nested);

        // put the second nested to the 3rd column in the second row
        //
        Mockito.when(upperSpr.getValue(1, 0)).thenReturn("arrayNested");
        Mockito.when(upperSpr.getValue(1, 2)).thenReturn(array);
        return upperSpr;
    }

    private SpreadsheetResult getSpreadsheet(String[] columns, int width, int height) {
        SpreadsheetResult spreadsheet = Mockito.mock(SpreadsheetResult.class);
        Mockito.when(spreadsheet.getWidth()).thenReturn(width);
        Mockito.when(spreadsheet.getHeight()).thenReturn(height);
        Mockito.when(spreadsheet.getColumnNames()).thenReturn(columns);
        return spreadsheet;
    }
}
