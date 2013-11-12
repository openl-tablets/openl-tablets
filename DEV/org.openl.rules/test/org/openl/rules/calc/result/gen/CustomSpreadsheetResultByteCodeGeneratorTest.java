package org.openl.rules.calc.result.gen;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
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
        
        Class<?> clazz = checkLoadingClass(className);    
        
        instantiate(clazz);          
    }
    
    @Test
    public void testSpreadsheetResult() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(SpreadsheetResult[].class));        
        
        String className = "my.test.CustomSpreadsheetResSprRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> clazz = checkLoadingClass(className);    
        
        instantiate(clazz); 
    }
    
    @Test
    public void testInt() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(int.class));        
        
        String className = "my.test.CustomSpreadsheetIntRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> clazz = checkLoadingClass(className);    
        
        instantiate(clazz); 
    }
    
    @Test
    public void testDouble() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(double.class));        
        
        String className = "my.test.CustomSpreadsheetDoubleRes";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> clazz = checkLoadingClass(className);    
        
        instantiate(clazz);        
    }
    
    @Test
    public void testRestrictedFieldName1() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final Value", new FieldDescription(double.class)); // not allowed name of field, should be skipped in result class
        cells.put("$Formula$Final_Value", new FieldDescription(String.class));
        
        String className = "my.test.CustomSpreadsheetRestricted1";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> clazz = checkLoadingClass(className);    
        
        instantiate(clazz);           
    }
    
    @Test
    public void testRestrictedFieldName2() {
        String field1 = "$Formula//$FinalValue";
        String field2 = "$Formula$Final_Value";
        
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put(field1, new FieldDescription(double.class)); // not allowed name of field, should be skipped in result class
        cells.put(field2, new FieldDescription(String.class));
        
        Map<String, Point> fieldCoordinates = new HashMap<String, Point>();
        fieldCoordinates.put(field1, new Point(0, 0));
        fieldCoordinates.put(field2, new Point(1, 0));
        
        String className = "my.test.CustomSpreadsheetRestricted2";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells, fieldCoordinates);
        gen.generateAndLoadBeanClass();
        
        Class<?> clazz = checkLoadingClass(className);    
        
        instantiate(clazz);
    }
    
    @Test
    public void testPrimitiveAray() {
        Map<String, FieldDescription> cells = new HashMap<String, FieldDescription>();
        cells.put("$Formula$Final_Value", new FieldDescription(int[].class));        
        
        String className = "my.test.CustomSpreadsheetPrimitiveArray";
        CustomSpreadsheetResultByteCodeGenerator gen = new CustomSpreadsheetResultByteCodeGenerator(className, cells);
        gen.generateAndLoadBeanClass();
        
        Class<?> clazz = checkLoadingClass(className);    
        
        instantiate(clazz);        
    }

    private Object instantiate(Class<?> clazz) {
        Object instance = null;
        Constructor<?> constructor = null;
        try {
            constructor = clazz.getConstructor(Object[][].class, String[].class, String[].class, Map.class);
        } catch (Exception e1) {
            fail();
        }       
        try {
            instance = constructor.newInstance(new Object[][]{}, new String[]{}, new String[]{}, new HashMap<String, Point>());
        } catch (Exception e) {
            fail();
        } 
        assertNotNull(instance);
        return instance;
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
