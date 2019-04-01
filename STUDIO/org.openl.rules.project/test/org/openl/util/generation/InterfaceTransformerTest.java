package org.openl.util.generation;

import static org.junit.Assert.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.openl.util.ClassUtils;
import org.springframework.core.annotation.AnnotationUtils;

public class InterfaceTransformerTest {
    private Class<?> getGeneratedClass() throws Exception {
        String className = "org.openl.rules.GeneratedInterface";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            ClassWriter classWriter = new ClassWriter(0);

            InterfaceTransformer transformer = new InterfaceTransformer(TestInterface.class, className);
            transformer.accept(classWriter);
            classWriter.visitEnd();

            ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);

            return Class.forName(className, true, classLoader);
        }
    }

    @Test
    public void testMethodsReading() throws Exception {
        Class<?> generated = getGeneratedClass();

        for (Method method : TestInterface.class.getMethods()) {
            Method methodInGenerated = generated.getMethod(method.getName(), method.getParameterTypes());
            assertNotNull(methodInGenerated);
            assertEquals(method.getReturnType(), methodInGenerated.getReturnType());
            assertEquals(method.getModifiers(), methodInGenerated.getModifiers());
        }
    }

    @Test
    public void testAnnotationsReading() throws Exception {
        Class<?> generated = getGeneratedClass();

        checkAnnotations(TestInterface.class, generated);

        for (Method method : TestInterface.class.getMethods()) {
            Method methodInGenerated = generated.getMethod(method.getName(), method.getParameterTypes());
            assertNotNull(methodInGenerated);
            checkAnnotations(method, methodInGenerated);
        }
        for (Field field : TestInterface.class.getFields()) {
            Field fieldGenerated = generated.getField(field.getName());
            assertNotNull(fieldGenerated);
            checkAnnotations(field, fieldGenerated);
        }
    }

    @Test
    public void testFieldsReading() throws Exception {
        Class<?> generated = getGeneratedClass();

        for (Field field : TestInterface.class.getFields()) {
            Field fieldGenerated = generated.getField(field.getName());
            assertNotNull(fieldGenerated);
            assertEquals(field.getType(), fieldGenerated.getType());
            assertEquals(field.getModifiers(), fieldGenerated.getModifiers());
            assertEquals(field.get(null), fieldGenerated.get(null));
        }
    }

    private static void checkAnnotations(AnnotatedElement original, AnnotatedElement generated) {
        for (Annotation annotation : original.getAnnotations()) {
            Annotation annotationGenerated = generated.getAnnotation(annotation.annotationType());
            assertNotNull(annotationGenerated);
            Map<String, Object> attributesOriginal = AnnotationUtils.getAnnotationAttributes(annotation);
            Map<String, Object> attributesGenerated = AnnotationUtils.getAnnotationAttributes(annotationGenerated);
            Set<String> keys = new HashSet<>();
            keys.addAll(attributesOriginal.keySet());
            keys.addAll(attributesGenerated.keySet());
            for (String key : keys) {
                Object valueOriginal = attributesOriginal.get(key);
                Object valueGenerated = attributesGenerated.get(key);
                if (valueOriginal.getClass().isArray()) {
                    assertArrayEquals((Object[]) valueOriginal, (Object[]) valueGenerated);
                } else {
                    assertEquals(valueOriginal, valueGenerated);
                }
            }
        }
    }

    @XmlType(name = "TestInterface", propOrder = { "const2", "const1" })
    public interface TestInterface {
        @XmlAttribute(name = "int_const")
        int const1 = 0;
        @XmlTransient
        String const2 = "test";

        String testMethod1();

        @Deprecated
        void testMethod1(String arg0);

        void testMethod1(String arg0, int arg1);

        @XmlTransient
        int testMethod2(double arg0);
    }
}
