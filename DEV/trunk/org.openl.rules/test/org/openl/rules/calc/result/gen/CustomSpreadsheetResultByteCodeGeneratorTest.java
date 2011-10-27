package org.openl.rules.calc.result.gen;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.datatype.gen.FieldDescription;

public class CustomSpreadsheetResultByteCodeGeneratorTest {
    @Test
    public void test1() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(org.openl.meta.StringValue.class));
        cells.put("$Value$Abra", new FieldDescription(org.openl.meta.DoubleValue.class));
        
        String className = "my.test.CustomSpreadsheetRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> generatedClass = null;
        try {
            generatedClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail();            
        }
        assertNotNull(generatedClass);
        generatedClass.getDeclaredFields();
        
    }
    
    @Test
    public void testArray() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(org.openl.meta.StringValue[].class));        
        
        String className = "my.test.CustomSpreadsheetResArray";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> generatedClass = null;
        try {
            generatedClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail();            
        }
        assertNotNull(generatedClass);
        generatedClass.getDeclaredFields();
        
    }
    
    @Test
    public void testSpreadsheetResult() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(SpreadsheetResult[].class));        
        
        String className = "my.test.CustomSpreadsheetResSprRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> generatedClass = null;
        try {
            generatedClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail();            
        }
        assertNotNull(generatedClass);
        generatedClass.getDeclaredFields();
        
    }
}
