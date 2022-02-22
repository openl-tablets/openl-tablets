package org.openl.util.generation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.openl.util.ClassUtils;

public class InterfaceTransformerTest {
    private Class<?> getGeneratedClass() throws Exception {
        String className = "org.openl.rules.GeneratedInterface";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            ClassWriter classWriter = new ClassWriter(0);

            InterfaceTransformer transformer = new InterfaceTransformer(getGeneratedClass1(), className);
            transformer.accept(classWriter);
            classWriter.visitEnd();

            ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);

            return Class.forName(className, true, classLoader);
        }
    }

    // This functionality check that recursive transformation works well too
    private Class<?> getGeneratedClass1() throws Exception {
        String className = "org.openl.rules.GeneratedInterface1";
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
            Set<String> keys = new HashSet<>();
            keys.addAll(Arrays.stream(annotation.annotationType().getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet()));
            keys.addAll(Arrays.stream(annotationGenerated.annotationType().getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet()));
            for (String key : keys) {
                Object valueOriginal;
                try {
                    valueOriginal = annotation.annotationType().getMethod(key).invoke(annotation);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                Object valueGenerated;
                try {
                    valueGenerated = annotationGenerated.annotationType().getMethod(key).invoke(annotation);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
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

        @TestAnnotation(value = "test")
        String testMethod1();

        @Deprecated
        void testMethod1(String arg0);

        void testMethod1(String arg0, int arg1);

        @XmlTransient
        int testMethod2(double arg0);
    }

    public enum ParameterIn {
        DEFAULT(""),
        HEADER("header");

        private final String value;

        ParameterIn(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    @Target({ PARAMETER, METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface TestAnnotation {
        ParameterIn in() default ParameterIn.DEFAULT;

        String value();
    }
}
