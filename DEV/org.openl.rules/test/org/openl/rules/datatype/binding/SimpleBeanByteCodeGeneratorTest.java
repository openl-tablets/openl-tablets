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
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.SimpleBeanByteCodeGenerator;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;

public class SimpleBeanByteCodeGeneratorTest {
    
    private final String CLASS_NAMESPACE = "my.test";
    
    @Test
    public void testStringFields() {
        String className = String.format("%s.StringBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(String.class.getName()));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDate() {
        String className = String.format("%s.DateBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(Date.class.getName()));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDoubleValue() {
        String className = String.format("%s.DoubleValueBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(DoubleValue.class.getName()));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testRanges() {
        String className = String.format("%s.RangesBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(IntRange.class.getName()));
        fields.putAll(getFields(new FieldDescription(DoubleRange.class.getName())));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testBigNumbers() {
        String className = String.format("%s.BigNumbersBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(BigInteger.class.getName()));
        fields.putAll(getFields(new FieldDescription(BigDecimal.class.getName())));
         
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testIntFields() {
        String className = String.format("%s.IntBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(int.class.getName()));
        fields.putAll(getFields(new FieldDescription(Integer.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testFloatFields() {
        String className = String.format("%s.FloatBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(float.class.getName()));
        fields.putAll(getFields(new FieldDescription(Float.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testBooleanFields() {
        String className = String.format("%s.BooleanBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(boolean.class.getName()));
        fields.putAll(getFields(new FieldDescription(Boolean.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testCharFields() {
        String className = String.format("%s.CharBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(char.class.getName()));
        fields.putAll(getFields(new FieldDescription(Character.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testByteFields() {
        String className = String.format("%s.ByteBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(byte.class.getName()));
        fields.putAll(getFields(new FieldDescription(Byte.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testShortFields() {
        String className = String.format("%s.ShortBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(short.class.getName()));
        fields.putAll(getFields(new FieldDescription(Short.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testLongFields() {
        String className = String.format("%s.LongBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(long.class.getName()));
        fields.putAll(getFields(new FieldDescription(Long.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testDoubleFields() {
        String className = String.format("%s.DoubleBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(double.class.getName()));
        fields.putAll(getFields(new FieldDescription(Double.class.getName())));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testIntArrayFields() {        
        String className = String.format("%s.IntArrayBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(int[].class.getName()));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testLongArrayFields() {        
        String className = String.format("%s.LongArrayBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(long[].class.getName()));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    @Test
    public void testObjectArrayFields() {        
        String className = String.format("%s.ObjectArrayBean", CLASS_NAMESPACE);
        Map<String, FieldDescription> fields = getFields(new FieldDescription(Object[].class.getName()));
        
        assertNotNull(getBeanClass(className, fields));
    }
    
    
    @Test
    public void testEquals() {
    	String className = String.format("%s.EqualsTestBean", CLASS_NAMESPACE);
    	Map<String, FieldDescription> fields = getFields(new FieldDescription(String.class.getName()));
        
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
