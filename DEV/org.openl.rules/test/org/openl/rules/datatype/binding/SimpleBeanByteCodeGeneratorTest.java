package org.openl.rules.datatype.binding;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.meta.DoubleValue;
import org.openl.rules.datatype.gen.DefaultFieldDescription;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.SimpleBeanByteCodeGenerator;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;

public class SimpleBeanByteCodeGeneratorTest {
    
    private final String CLASS_NAMESPACE = "my.test";
    
    @Test
    public void testStringFields() {
        String className = String.format("%s.StringBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(String.class));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDate() {
        String className = String.format("%s.DateBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(Date.class));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDoubleValue() {
        String className = String.format("%s.DoubleValueBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(DoubleValue.class));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testRanges() {
        String className = String.format("%s.RangesBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(IntRange.class));
        fields.putAll(getFields(new DefaultFieldDescription(DoubleRange.class)));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testBigNumbers() {
        String className = String.format("%s.BigNumbersBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(BigInteger.class));
        fields.putAll(getFields(new DefaultFieldDescription(BigDecimal.class)));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testIntFields() {
        String className = String.format("%s.IntBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(int.class));
        fields.putAll(getFields(new DefaultFieldDescription(Integer.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testFloatFields() {
        String className = String.format("%s.FloatBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(float.class));
        fields.putAll(getFields(new DefaultFieldDescription(Float.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testBooleanFields() {
        String className = String.format("%s.BooleanBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(boolean.class));
        fields.putAll(getFields(new DefaultFieldDescription(Boolean.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testCharFields() {
        String className = String.format("%s.CharBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(char.class));
        fields.putAll(getFields(new DefaultFieldDescription(Character.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testByteFields() {
        String className = String.format("%s.ByteBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(byte.class));
        fields.putAll(getFields(new DefaultFieldDescription(Byte.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testShortFields() {
        String className = String.format("%s.ShortBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(short.class));
        fields.putAll(getFields(new DefaultFieldDescription(Short.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testLongFields() {
        String className = String.format("%s.LongBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(long.class));
        fields.putAll(getFields(new DefaultFieldDescription(Long.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDoubleFields() {
        String className = String.format("%s.DoubleBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(double.class));
        fields.putAll(getFields(new DefaultFieldDescription(Double.class)));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testIntArrayFields() {        
        String className = String.format("%s.IntArrayBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(int[].class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testLongArrayFields() {        
        String className = String.format("%s.LongArrayBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(long[].class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testObjectArrayFields() {        
        String className = String.format("%s.ObjectArrayBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(Object[].class));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    
    @Test
    public void testEquals() {
    	String className = String.format("%s.EqualsTestBean", CLASS_NAMESPACE);
    	Map<String, FieldDescription> fields = getFields(new DefaultFieldDescription(String.class));
        
    	Class<?> clazz = getBeanClass(className, fields);
        assertNotNull(clazz);
        
        Object instance1 = null, instance2 = null;
        Method equalsMethod = null, setFirstField = null;
        Boolean isEqual = false;
        try {
			 instance1 = clazz.newInstance();
			 instance2 = clazz.newInstance();
			 equalsMethod = clazz.getMethod("equals", new Class[]{Object.class});
			 setFirstField = clazz.getMethod("setFirstField", new Class[]{String.class});
			 isEqual = (Boolean)equalsMethod.invoke(instance1, new Object[]{instance2});
		} catch (Exception e) {
			fail(e.getMessage());
		}
        assertTrue(isEqual);
        
        //set field value for one of the instances
        try {
        	assertNotNull(setFirstField);
        	setFirstField.invoke(instance1, new Object[]{"TestValue"});
        	isEqual = (Boolean)equalsMethod.invoke(instance1, new Object[]{instance2});
        } catch (Exception e) {
        	fail(e.getMessage());
		}
        assertFalse(isEqual);
    }
    
    private Class<?> getBeanClass(String className, Map<String, FieldDescription> fields) {
        SimpleBundleClassLoader simpleBundleClassLoader = new SimpleBundleClassLoader(Thread.currentThread().getContextClassLoader());
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try{
            Thread.currentThread().setContextClassLoader(simpleBundleClassLoader);
            SimpleBeanByteCodeGenerator beanGenerator = new SimpleBeanByteCodeGenerator(className, fields);
            Class<?> beanClass = beanGenerator.generateAndLoadBeanClass();
            return beanClass;
        }finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private Map<String, FieldDescription> getFields(FieldDescription fieldsType) {
        Map<String, FieldDescription> fields = new HashMap<String, FieldDescription>();
        fields.put("firstField", fieldsType);
        fields.put("secondField", fieldsType);
        return fields;
    }
}
