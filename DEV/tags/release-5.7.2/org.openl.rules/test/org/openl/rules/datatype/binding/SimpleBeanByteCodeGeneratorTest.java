package org.openl.rules.datatype.binding;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;

public class SimpleBeanByteCodeGeneratorTest {
    
    private final String CLASS_NAMESPACE = "my.test";
    
    @Test
    public void testStringFields() {
        String className = String.format("%s.StringBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(String.class.getSimpleName(), String.class));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDate() {
        String className = String.format("%s.DateBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(Date.class.getSimpleName(), Date.class));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDoubleValue() {
        String className = String.format("%s.DoubleValueBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(DoubleValue.class.getSimpleName(), DoubleValue.class));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testRanges() {
        String className = String.format("%s.RangesBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(IntRange.class.getSimpleName(), IntRange.class));
        fields.putAll(getFields(new FieldType(DoubleRange.class.getSimpleName(), DoubleRange.class)));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testBigNumbers() {
        String className = String.format("%s.BigNumbersBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(BigInteger.class.getSimpleName(), BigInteger.class));
        fields.putAll(getFields(new FieldType(BigDecimal.class.getSimpleName(), BigDecimal.class)));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testIntFields() {
        String className = String.format("%s.IntBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(int.class.getSimpleName(), int.class));
        fields.putAll(getFields(new FieldType(Integer.class.getSimpleName(), Integer.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testFloatFields() {
        String className = String.format("%s.FloatBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(float.class.getSimpleName(), float.class));
        fields.putAll(getFields(new FieldType(Float.class.getSimpleName(), Float.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testBooleanFields() {
        String className = String.format("%s.BooleanBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(boolean.class.getSimpleName(), boolean.class));
        fields.putAll(getFields(new FieldType(Boolean.class.getSimpleName(), Boolean.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testCharFields() {
        String className = String.format("%s.CharBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(char.class.getSimpleName(), char.class));
        fields.putAll(getFields(new FieldType(Character.class.getSimpleName(), Character.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testByteFields() {
        String className = String.format("%s.ByteBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(byte.class.getSimpleName(), byte.class));
        fields.putAll(getFields(new FieldType(Byte.class.getSimpleName(), Byte.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testShortFields() {
        String className = String.format("%s.ShortBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(short.class.getSimpleName(), short.class));
        fields.putAll(getFields(new FieldType(Short.class.getSimpleName(), Short.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testLongFields() {
        String className = String.format("%s.LongBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(long.class.getSimpleName(), long.class));
        fields.putAll(getFields(new FieldType(Long.class.getSimpleName(), Long.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDoubleFields() {
        String className = String.format("%s.DoubleBean", CLASS_NAMESPACE);
        Map<String, FieldType> fields = getFields(new FieldType(double.class.getSimpleName(), double.class));
        fields.putAll(getFields(new FieldType(Double.class.getSimpleName(), Double.class)));
        
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
