package org.openl.rules.calc.result.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SpreadsheetColumnExtractorTest {
    
    @Test
    public void testSetterName() {
        String ecpectedName1 = "setId";        
        SpreadsheetColumnExtractor<CalculationStep> extractor = new SpreadsheetColumnExtractor<CalculationStep>(null, true); 
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
    
    @Test 
    public void testNotExistingColumn() {
    	 String testedValue = "valueToExtract";
    	 ColumnToExtract columnToExtract = new ColumnToExtract("Not_Existing_Column", String.class, false);
         SpreadsheetColumnExtractor<CodeStep> extractor = new SpreadsheetColumnExtractor<CodeStep>(columnToExtract, true);
         
         CodeStep instanceToPopulate = new CodeStep();
         // storing with converting
         extractor.convertAndStoreData(new org.openl.meta.StringValue(testedValue), instanceToPopulate);        
         assertNull(instanceToPopulate.getCode());
    }
    
    @Test
    public void testExtractingValueOfNotAppropriateType() {
    	ColumnToExtract columnToExtract = new ColumnToExtract("Value", Double.class, false);
    	SpreadsheetColumnExtractor<CodeStep> extractor = new SpreadsheetColumnExtractor<CodeStep>(columnToExtract, true);
    	
    	SimpleStep instanceToPopulate = new SimpleStep();
    	
    	extractor.convertAndStoreData("stringValueInsteadOfDouble", instanceToPopulate);        
        assertNull(instanceToPopulate.getValue());
    	
    }
}
