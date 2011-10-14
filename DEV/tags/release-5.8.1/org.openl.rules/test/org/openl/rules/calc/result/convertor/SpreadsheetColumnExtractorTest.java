package org.openl.rules.calc.result.convertor;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.calc.result.convertor.CalculationStep;
import org.openl.rules.calc.result.convertor.SpreadsheetColumnExtractor;

public class SpreadsheetColumnExtractorTest {
    
    @Test
    public void testSetterName() {
        String ecpectedName1 = "setId";        
        SpreadsheetColumnExtractor<CalculationStep> extractor = new SpreadsheetColumnExtractor<CalculationStep>(null, true); 
        assertEquals(ecpectedName1, extractor.getSetterName("id"));
        assertEquals(ecpectedName1, extractor.getSetterName("ID"));
        assertEquals(ecpectedName1, extractor.getSetterName("Id"));
        assertEquals(ecpectedName1, extractor.getSetterName("iD"));
    }
    
    @Test
    public void testColumnExtractor() {
        String testedValue = "valueToExtract";
        ColumnToExtract columnToExtract = new ColumnToExtract("Code", String.class, false);
        SpreadsheetColumnExtractor<CodeStep> extractor = new SpreadsheetColumnExtractor<CodeStep>(columnToExtract, true);
        
        CodeStep instanceToPopulate = new CodeStep();
        // storing with converting
        extractor.convertAndStoreData(new org.openl.meta.StringValue(testedValue), instanceToPopulate);        
        assertEquals(testedValue, instanceToPopulate.getCode());
        
        CodeStep instanceToPopulate1 = new CodeStep();
        // storing without converting
        extractor.convertAndStoreData(testedValue, instanceToPopulate1);
        
        assertEquals(testedValue, instanceToPopulate.getCode());
    }
}
