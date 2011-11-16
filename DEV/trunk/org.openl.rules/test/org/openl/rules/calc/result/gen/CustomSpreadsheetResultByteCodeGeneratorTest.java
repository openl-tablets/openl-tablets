package org.openl.rules.calc.result.gen;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.table.Point;

public class CustomSpreadsheetResultByteCodeGeneratorTest {
    @Test
    public void test1() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        String field1 = "$Formula$Final_Value";
        String field2 = "$Value$Abra";
        
        cells.put(field1, new FieldDescription(org.openl.meta.StringValue.class));        
        cells.put(field2, new FieldDescription(org.openl.meta.DoubleValue.class));
        
        Map<String, Point> fieldCoordinates = new HashMap<String, Point>();
        fieldCoordinates.put(field1, new Point(0, 0));
        fieldCoordinates.put(field2, new Point(0, 1));        
        
        String className = "my.test.CustomSpreadsheetRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells, fieldCoordinates);
        gen.generateAndLoadBeanClass();
        
        Class<?> clazz = checkLoadingClass(className);    
        
        Object instance = null;
        try {
            instance = clazz.newInstance();
        } catch (Exception e) {            
            fail();
        } 
        assertNotNull(instance);
        
    }
    
    @Test
    public void testArray() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(org.openl.meta.StringValue[].class));        
        
        String className = "my.test.CustomSpreadsheetResArray";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        checkLoadingClass(className);          
    }
    
    @Test
    public void testSpreadsheetResult() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(SpreadsheetResult[].class));        
        
        String className = "my.test.CustomSpreadsheetResSprRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        checkLoadingClass(className);  
    }
    
    @Test
    public void testInt() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(int.class));        
        
        String className = "my.test.CustomSpreadsheetIntRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        checkLoadingClass(className);  
        
    }
    
    @Test
    public void testDouble() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(double.class));        
        
        String className = "my.test.CustomSpreadsheetDoubleRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        checkLoadingClass(className);          
    }
    
    @Test
    public void testPrimitiveAray() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(int[].class));        
        
        String className = "my.test.CustomSpreadsheetPrimitiveArray";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        checkLoadingClass(className);        
    }

    private Class<?> checkLoadingClass(String className) {
        Class<?> generatedClass = null;
        try {
            generatedClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail();            
        }
        assertNotNull(generatedClass);
        generatedClass.getDeclaredFields();
        return generatedClass;
    }
    
    
}
