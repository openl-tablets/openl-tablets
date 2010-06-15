package org.openl.rules.datatype.binding;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SimpleBeanByteCodeGeneratorTest {
    
    private final String CLASS_NAMESPACE = "my.test";
    
    @Test
    public void testStringFields() {
        String className = String.format("%s.StringBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(String.class.getSimpleName(), String.class));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testIntFields() {
        String className = String.format("%s.IntBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(int.class.getSimpleName(), int.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testFloatFields() {
        String className = String.format("%s.FloatBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(float.class.getSimpleName(), float.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testBooleanFields() {
        String className = String.format("%s.BooleanBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(boolean.class.getSimpleName(), boolean.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testCharFields() {
        String className = String.format("%s.CharBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(char.class.getSimpleName(), char.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testByteFields() {
        String className = String.format("%s.ByteBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(byte.class.getSimpleName(), byte.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testShortFields() {
        String className = String.format("%s.ShortBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(short.class.getSimpleName(), short.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testLongFields() {
        String className = String.format("%s.LongBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(long.class.getSimpleName(), long.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDoubleFields() {
        String className = String.format("%s.DoubleBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(double.class.getSimpleName(), double.class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testIntArrayFields() {        
        String className = String.format("%s.IntArrayBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(int[].class.getSimpleName(), int[].class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testLongArrayFields() {        
        String className = String.format("%s.LongArrayBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(long[].class.getSimpleName(), long[].class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testObjectArrayFields() {        
        String className = String.format("%s.ObjectArrayBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(Object[].class.getSimpleName(), Object[].class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    private Class<?> getBeanClass(String className, Map<String, FieldType> fields) {
        SimpleBeanByteCodeGenerator beanGenerator = new SimpleBeanByteCodeGenerator(className, fields);
        Class<?> beanClass = beanGenerator.generateAndLoadBeanClass();
        return beanClass;
    }

    private Map<String, FieldType> getFields(FieldType fieldsType) {
        Map<String, FieldType> fields = new HashMap<String, FieldType>();
        fields.put("firstField", fieldsType);
        fields.put("secondField", fieldsType);
        return fields;
    }
}
