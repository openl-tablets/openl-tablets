package org.openl.rules.calculation.result.convertor2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class SpreadsheetColumnExtractorTest {

    private NestedSpreadsheetConfiguration<SimpleStep, CompoundStep> getConfiguration() {
        return new NestedSpreadsheetConfiguration<SimpleStep, CompoundStep>(new HashMap<>()) {
            @Override
            protected RowExtractor<CompoundStep> initCompoundRowExtractor(
                    List<SpreadsheetColumnExtractor<CompoundStep>> compoundExtractors) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected RowExtractor<SimpleStep> initSimpleRowExtractor(
                    List<SpreadsheetColumnExtractor<SimpleStep>> simpleExtractors) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected NestedSpreadsheedColumnExtractor initCompoundColumnExtractor(int nestingLevel,
                    ColumnToExtract column) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    public void testSetterName() {
        String ecpectedName1 = "setId";
        SpreadsheetColumnExtractor<CodeStep> extractor = new SpreadsheetColumnExtractor<>(null, getConfiguration());
        assertEquals(ecpectedName1, extractor.getSetterName("id"));
        assertEquals(ecpectedName1, extractor.getSetterName("ID"));
        assertEquals(ecpectedName1, extractor.getSetterName("Id"));
        assertEquals(ecpectedName1, extractor.getSetterName("iD"));
        assertEquals("setCode", extractor.getSetterName("CoDe"));
        assertEquals("setCode", extractor.getSetterName("code"));
        assertEquals("setMycustomsuperfield", extractor.getSetterName("myCustomSuperField"));
    }

    @Test
    public void testColumnExtractor() {
        String testedValue = "valueToExtract";
        ColumnToExtract columnToExtract = new ColumnToExtract("Code", String.class);
        SpreadsheetColumnExtractor<CodeStep> extractor = new SpreadsheetColumnExtractor<>(columnToExtract,
            getConfiguration());

        CodeStep instanceToPopulate = new CodeStep();
        // storing with converting
        extractor.convertAndStoreData(new org.openl.meta.StringValue(testedValue), instanceToPopulate);
        assertEquals(testedValue, instanceToPopulate.getCode());

        CodeStep instanceToPopulate1 = new CodeStep();
        // storing without converting
        extractor.convertAndStoreData(testedValue, instanceToPopulate1);

        assertEquals(testedValue, instanceToPopulate.getCode());
    }

    @Test
    public void testNotExistingColumn() {
        String testedValue = "valueToExtract";
        ColumnToExtract columnToExtract = new ColumnToExtract("Not_Existing_Column", String.class);
        SpreadsheetColumnExtractor<CodeStep> extractor = new SpreadsheetColumnExtractor<>(columnToExtract,
            getConfiguration());

        CodeStep instanceToPopulate = new CodeStep();
        // storing with converting
        extractor.convertAndStoreData(new org.openl.meta.StringValue(testedValue), instanceToPopulate);
        assertNull(instanceToPopulate.getCode());
    }

    @Test
    public void testExtractingValueOfNotAppropriateType() {
        ColumnToExtract columnToExtract = new ColumnToExtract("Value", Double.class);
        SpreadsheetColumnExtractor<CodeStep> extractor = new SpreadsheetColumnExtractor<>(columnToExtract,
            getConfiguration());

        SimpleStep instanceToPopulate = new SimpleStep();

        extractor.convertAndStoreData("stringValueInsteadOfDouble", instanceToPopulate);
        assertNull(instanceToPopulate.getValue());

    }
}
