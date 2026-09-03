package org.openl.rules.datatype.binding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;

class SimpleBeanByteCodeGeneratorTest {

    @Test
    void testStringFields() {
        var fields = getFields(new FieldDescription(String.class.getName()));
        assertNotNull(getBeanClass("my.test.StringBean", fields));
    }

    @Test
    void testDate() {
        var fields = getFields(new FieldDescription(Date.class.getName()));
        assertNotNull(getBeanClass("my.test.DateBean", fields));
    }

    @Test
    void testDoubleValue() {
        var fields = getFields(new FieldDescription(Double.class.getName()));
        assertNotNull(getBeanClass("my.test.DoubleValueBean", fields));
    }

    @Test
    void testRanges() {
        var fields = getFields(new FieldDescription(IntRange.class.getName()));
        fields.putAll(getFields(new FieldDescription(DoubleRange.class.getName())));
        assertNotNull(getBeanClass("my.test.RangesBean", fields));
    }

    @Test
    void testBigNumbers() {
        var fields = getFields(new FieldDescription(BigInteger.class.getName()));
        fields.putAll(getFields(new FieldDescription(BigDecimal.class.getName())));
        assertNotNull(getBeanClass("my.test.BigNumbersBean", fields));
    }

    @Test
    void testIntFields() {
        var fields = getFields(new FieldDescription(int.class.getName()));
        fields.putAll(getFields(new FieldDescription(Integer.class.getName())));
        assertNotNull(getBeanClass("my.test.IntBean", fields));
    }

    @Test
    void testFloatFields() {
        var fields = getFields(new FieldDescription(float.class.getName()));
        fields.putAll(getFields(new FieldDescription(Float.class.getName())));
        assertNotNull(getBeanClass("my.test.FloatBean", fields));
    }

    @Test
    void testBooleanFields() {
        var fields = getFields(new FieldDescription(boolean.class.getName()));
        fields.putAll(getFields(new FieldDescription(Boolean.class.getName())));
        assertNotNull(getBeanClass("my.test.BooleanBean", fields));
    }

    @Test
    void testCharFields() {
        var fields = getFields(new FieldDescription(char.class.getName()));
        fields.putAll(getFields(new FieldDescription(Character.class.getName())));
        assertNotNull(getBeanClass("my.test.CharBean", fields));
    }

    @Test
    void testByteFields() {
        var fields = getFields(new FieldDescription(byte.class.getName()));
        fields.putAll(getFields(new FieldDescription(Byte.class.getName())));
        assertNotNull(getBeanClass("my.test.ByteBean", fields));
    }

    @Test
    void testShortFields() {
        var fields = getFields(new FieldDescription(short.class.getName()));
        fields.putAll(getFields(new FieldDescription(Short.class.getName())));
        assertNotNull(getBeanClass("my.test.ShortBean", fields));
    }

    @Test
    void testLongFields() {
        var fields = getFields(new FieldDescription(long.class.getName()));
        fields.putAll(getFields(new FieldDescription(Long.class.getName())));
        assertNotNull(getBeanClass("my.test.LongBean", fields));
    }

    @Test
    void testDoubleFields() {
        var fields = getFields(new FieldDescription(double.class.getName()));
        fields.putAll(getFields(new FieldDescription(Double.class.getName())));
        assertNotNull(getBeanClass("my.test.DoubleBean", fields));
    }

    @Test
    void testIntArrayFields() {
        var fields = getFields(new FieldDescription(int[].class.getName()));
        assertNotNull(getBeanClass("my.test.IntArrayBean", fields));
    }

    @Test
    void testLongArrayFields() {
        var fields = getFields(new FieldDescription(long[].class.getName()));
        assertNotNull(getBeanClass("my.test.LongArrayBean", fields));
    }

    @Test
    void testObjectArrayFields() {
        var fields = getFields(new FieldDescription(Object[].class.getName()));
        assertNotNull(getBeanClass("my.test.ObjectArrayBean", fields));
    }

    @Test
    void testEquals() {
        var fields = getFields(new FieldDescription(String.class.getName()));

        var clazz = getBeanClass("my.test.EqualsTestBean", fields);
        assertNotNull(clazz);

        Object instance1 = null, instance2 = null;
        Method equalsMethod = null, setFirstField = null;
        var isEqual = false;
        try {
            instance1 = clazz.getDeclaredConstructor().newInstance();
            instance2 = clazz.getDeclaredConstructor().newInstance();
            equalsMethod = clazz.getMethod("equals", Object.class);
            setFirstField = clazz.getMethod("setFirstField", String.class);
            isEqual = (Boolean) equalsMethod.invoke(instance1, instance2);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertTrue(isEqual);

        // set field value for one of the instances
        try {
            assertNotNull(setFirstField);
            setFirstField.invoke(instance1, "TestValue");
            isEqual = (Boolean) equalsMethod.invoke(instance1, instance2);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertFalse(isEqual);
    }

    private Class<?> getBeanClass(String className, Map<String, FieldDescription> fields) {
        var simpleClassLoader = new OpenLClassLoader(
                Thread.currentThread().getContextClassLoader());
        var oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(simpleClassLoader);
            var beanBuilder = new JavaBeanClassBuilder(className);
            beanBuilder.addFields(fields);
            var byteCode = beanBuilder.byteCode();
            return ClassLoaderUtils.defineClass(className, byteCode, simpleClassLoader);
        } catch (Exception e) {
            fail();
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private Map<String, FieldDescription> getFields(FieldDescription fieldsType) {
        var fields = new HashMap<String, FieldDescription>();
        fields.put("firstField", fieldsType);
        fields.put("secondField", fieldsType);
        return fields;
    }
}
