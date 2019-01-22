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
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.util.ClassUtils;

public class SimpleBeanByteCodeGeneratorTest {
    
    @Test
    public void testStringFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(String.class.getName()));
        assertNotNull(getBeanClass("my.test.StringBean", fields));
    }
    
    @Test
    public void testDate() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(Date.class.getName()));
        assertNotNull(getBeanClass("my.test.DateBean", fields));
    }
    
    @Test
    public void testDoubleValue() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(DoubleValue.class.getName()));
        assertNotNull(getBeanClass("my.test.DoubleValueBean", fields));
    }
    
    @Test
    public void testRanges() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(IntRange.class.getName()));
        fields.putAll(getFields(new FieldDescription(DoubleRange.class.getName())));
        assertNotNull(getBeanClass("my.test.RangesBean", fields));
    }
    
    @Test
    public void testBigNumbers() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(BigInteger.class.getName()));
        fields.putAll(getFields(new FieldDescription(BigDecimal.class.getName())));
        assertNotNull(getBeanClass("my.test.BigNumbersBean", fields));
    }
    
    @Test
    public void testIntFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(int.class.getName()));
        fields.putAll(getFields(new FieldDescription(Integer.class.getName())));
        assertNotNull(getBeanClass("my.test.IntBean", fields));
    }
    
    @Test
    public void testFloatFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(float.class.getName()));
        fields.putAll(getFields(new FieldDescription(Float.class.getName())));
        assertNotNull(getBeanClass("my.test.FloatBean", fields));
    }
    
    @Test
    public void testBooleanFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(boolean.class.getName()));
        fields.putAll(getFields(new FieldDescription(Boolean.class.getName())));
        assertNotNull(getBeanClass("my.test.BooleanBean", fields));
    }
    
    @Test
    public void testCharFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(char.class.getName()));
        fields.putAll(getFields(new FieldDescription(Character.class.getName())));
        assertNotNull(getBeanClass("my.test.CharBean", fields));
    }
    
    @Test
    public void testByteFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(byte.class.getName()));
        fields.putAll(getFields(new FieldDescription(Byte.class.getName())));
        assertNotNull(getBeanClass("my.test.ByteBean", fields));
    }
    
    @Test
    public void testShortFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(short.class.getName()));
        fields.putAll(getFields(new FieldDescription(Short.class.getName())));
        assertNotNull(getBeanClass("my.test.ShortBean", fields));
    }
    
    @Test
    public void testLongFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(long.class.getName()));
        fields.putAll(getFields(new FieldDescription(Long.class.getName())));
        assertNotNull(getBeanClass("my.test.LongBean", fields));
    }
    
    @Test
    public void testDoubleFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(double.class.getName()));
        fields.putAll(getFields(new FieldDescription(Double.class.getName())));
        assertNotNull(getBeanClass("my.test.DoubleBean", fields));
    }
    
    @Test
    public void testIntArrayFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(int[].class.getName()));
        assertNotNull(getBeanClass("my.test.IntArrayBean", fields));
    }
    
    @Test
    public void testLongArrayFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(long[].class.getName()));
        assertNotNull(getBeanClass("my.test.LongArrayBean", fields));
    }
    
    @Test
    public void testObjectArrayFields() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(Object[].class.getName()));
        assertNotNull(getBeanClass("my.test.ObjectArrayBean", fields));
    }
    
    
    @Test
    public void testEquals() {
        Map<String, FieldDescription> fields = getFields(new FieldDescription(String.class.getName()));
        
    	Class<?> clazz = getBeanClass("my.test.EqualsTestBean", fields);
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
        try {
            Thread.currentThread().setContextClassLoader(simpleBundleClassLoader);
            JavaBeanClassBuilder beanBuilder = new JavaBeanClassBuilder(className);
            beanBuilder.addFields(fields);
            byte[] byteCode = beanBuilder.byteCode();
            return ClassUtils.defineClass(className, byteCode, simpleBundleClassLoader);
        } catch (Exception e) {
            fail();
            return null;
        } finally {
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
