package org.openl.gen;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.openl.classloader.OpenLClassLoader;
import org.openl.gen.AnnotationDescription.AnnotationProperty;
import org.openl.util.ClassUtils;

public class JavaInterfaceByteCodeGeneratorTest {

    private static final Class<?>[] NO_ARGS = new Class<?>[0];

    @Test
    public void testGenerateEmpty() throws IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        final String expectedName = JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + "ServiceEmpty";
        JavaInterfaceByteCodeGenerator generator = new JavaInterfaceByteCodeGenerator(expectedName,
            Collections.emptyList());
        Class<?> interfaceClass = defineClass(expectedName, generator.byteCode());
        assertInterfaceDescription(expectedName, interfaceClass);
    }

    @Test
    public void testGenerateWithMethods() throws IllegalAccessException,
                                          InvocationTargetException,
                                          ClassNotFoundException,
                                          NoSuchMethodException {
        final String expectedName = JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + "ServiceWithMethods";
        final Class<?>[] args2 = new Class<?>[] { Object.class, int.class, Date.class };
        final JavaInterfaceByteCodeGenerator generator = new JavaInterfaceByteCodeGenerator(expectedName,
            Arrays.asList(new MethodDescription("doSomething", Object.class, NO_ARGS),
                new MethodDescription("doSomething2", int.class, args2),
                new MethodDescription("doSomething3", void.class, NO_ARGS)));

        final Class<?> interfaceClass = defineClass(expectedName, generator.byteCode());
        assertInterfaceDescription(expectedName, interfaceClass);

        final Method[] methods = interfaceClass.getDeclaredMethods();
        assertEquals(3, methods.length);

        assertSame(Object.class, interfaceClass.getMethod("doSomething").getReturnType());
        assertSame(int.class, interfaceClass.getMethod("doSomething2", args2).getReturnType());
        assertSame(void.class, interfaceClass.getMethod("doSomething3").getReturnType());
    }

    @Test
    public void testGenerateWithMethodsAndAnnotations() throws IllegalAccessException,
                                                        InvocationTargetException,
                                                        ClassNotFoundException,
                                                        NoSuchMethodException {
        final String expectedName = JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + "ServiceWithMethodsAndAnnotations";
        final Class<?>[] args2 = new Class<?>[] { Object.class, Object.class };
        final JavaInterfaceByteCodeGenerator generator = new JavaInterfaceByteCodeGenerator(expectedName,
            Collections.singletonList(new MethodDescription("doSomething",
                Object.class.getName(),
                toArray(new TypeDescription(Object.class.getName()),
                    new TypeDescription(Object.class.getName(),
                        toArray(new AnnotationDescription(MyAnnotation.class, null),
                            new AnnotationDescription(MyAnnotation2.class,
                                toArray(new AnnotationProperty("value", "foo")))))),
                toArray(new AnnotationDescription(MyAnnotation.class, null),
                    new AnnotationDescription(MyAnnotation2.class, toArray(new AnnotationProperty("value", "foo"))),
                    new AnnotationDescription(MyAnnotation3.class,
                        toArray(new AnnotationProperty("value", "foo"), new AnnotationProperty("field", "bar")))))));

        final Class<?> interfaceClass = defineClass(expectedName, generator.byteCode());
        assertInterfaceDescription(expectedName, interfaceClass);

        assertEquals(1, interfaceClass.getDeclaredMethods().length);
        final Method method = interfaceClass.getMethod("doSomething", args2);

        assertEquals(3, method.getAnnotations().length);
        assertNotNull(method.getAnnotation(MyAnnotation.class));

        MyAnnotation2 annotation2 = method.getAnnotation(MyAnnotation2.class);
        assertEquals("foo", annotation2.value());
        assertEquals("", annotation2.field());

        MyAnnotation3 annotation3 = method.getAnnotation(MyAnnotation3.class);
        assertEquals("foo", annotation3.value());
        assertEquals("bar", annotation3.field());

        assertEquals(0, method.getParameters()[0].getAnnotations().length);
        assertEquals(2, method.getParameters()[1].getAnnotations().length);
        assertNotNull(method.getParameters()[1].getAnnotation(MyAnnotation.class));

        MyAnnotation2 paramAnno2 = method.getParameters()[1].getAnnotation(MyAnnotation2.class);
        assertEquals("foo", paramAnno2.value());
        assertEquals("", paramAnno2.field());
    }

    @Test
    public void testGenerateWithMethodsAndAnnotationsBuilder2() throws IllegalAccessException,
                                                                InvocationTargetException,
                                                                ClassNotFoundException,
                                                                NoSuchMethodException {
        final String expectedName = JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + "ServiceWithMethodsAndAnnotations";
        final Class<?>[] args2 = new Class<?>[] { Object.class, Object.class };
        final JavaInterfaceByteCodeGenerator generator = InterfaceByteCodeBuilder
            .createWithDefaultPackage("ServiceWithMethodsAndAnnotations")
            .addAbstractMethod(MethodDescriptionBuilder.create("doSomething", Object.class)
                .addAnnotation(
                    AnnotationDescriptionBuilder.create(MyAnnotation4.class).withProperty("value", "foo", true).build())
                .addParameter(MethodParameterBuilder.create(Object.class).build())
                .addParameter(MethodParameterBuilder.create(Object.class)
                    .addAnnotation(AnnotationDescriptionBuilder.create(MyAnnotation4.class)
                        .withProperty("value", new String[] { "foo", "bar" })
                        .build())
                    .build())
                .build())
            .buildJava();

        final Class<?> interfaceClass = defineClass(expectedName, generator.byteCode());
        assertInterfaceDescription(expectedName, interfaceClass);

        assertEquals(1, interfaceClass.getDeclaredMethods().length);
        final Method method = interfaceClass.getMethod("doSomething", args2);

        assertEquals(1, method.getAnnotations().length);

        assertArrayEquals(new String[] { "foo" }, method.getAnnotation(MyAnnotation4.class).value());

        assertEquals(0, method.getParameters()[0].getAnnotations().length);
        assertEquals(1, method.getParameters()[1].getAnnotations().length);
        assertArrayEquals(new String[] { "foo", "bar" },
            method.getParameters()[1].getAnnotation(MyAnnotation4.class).value());
    }

    @Test
    public void testGenerateWithMethodsAndAnnotationsBuilder3() throws IllegalAccessException,
                                                                InvocationTargetException,
                                                                ClassNotFoundException,
                                                                NoSuchMethodException {
        final String expectedName = JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + "ServiceWithMethodsAndAnnotations";
        final Class<?>[] args2 = new Class<?>[] { Object.class, Object.class };
        final JavaInterfaceByteCodeGenerator generator = InterfaceByteCodeBuilder
            .createWithDefaultPackage("ServiceWithMethodsAndAnnotations")
            .addAbstractMethod(MethodDescriptionBuilder.create("doSomething", Object.class)
                .addAnnotation(AnnotationDescriptionBuilder.create(MyAnnotation5.class)
                    .withProperty("value", new TypeDescription(Integer[].class.getName()))
                    .build())
                .addParameter(MethodParameterBuilder.create(Object.class).build())
                .addParameter(MethodParameterBuilder.create(Object.class)
                    .addAnnotation(AnnotationDescriptionBuilder.create(MyAnnotation5.class)
                        .withProperty("value", new TypeDescription(Integer.class.getName()))
                        .build())
                    .build())
                .build())
            .buildJava();

        final Class<?> interfaceClass = defineClass(expectedName, generator.byteCode());
        assertInterfaceDescription(expectedName, interfaceClass);

        assertEquals(1, interfaceClass.getDeclaredMethods().length);
        final Method method = interfaceClass.getMethod("doSomething", args2);

        assertEquals(1, method.getAnnotations().length);

        assertEquals(Integer[].class, method.getAnnotation(MyAnnotation5.class).value());

        assertEquals(0, method.getParameters()[0].getAnnotations().length);
        assertEquals(1, method.getParameters()[1].getAnnotations().length);
        assertEquals(Integer.class, method.getParameters()[1].getAnnotation(MyAnnotation5.class).value());
    }

    @Test
    public void testGenerateWithMethodsAndAnnotationsBuilder() throws IllegalAccessException,
                                                               InvocationTargetException,
                                                               ClassNotFoundException,
                                                               NoSuchMethodException {
        final String expectedName = JavaInterfaceByteCodeGenerator.DEFAULT_PACKAGE + "ServiceWithMethodsAndAnnotations";
        final Class<?>[] args2 = new Class<?>[] { Object.class, Object.class };
        final JavaInterfaceByteCodeGenerator generator = InterfaceByteCodeBuilder
            .createWithDefaultPackage("ServiceWithMethodsAndAnnotations")
            .addAbstractMethod(MethodDescriptionBuilder.create("doSomething", Object.class)
                .addAnnotation(AnnotationDescriptionBuilder.create(MyAnnotation.class).build())
                .addAnnotation(
                    AnnotationDescriptionBuilder.create(MyAnnotation2.class).withProperty("value", "foo").build())
                .addAnnotation(AnnotationDescriptionBuilder.create(MyAnnotation3.class)
                    .withProperty("value", "foo")
                    .withProperty("field", "bar")
                    .build())
                .addParameter(MethodParameterBuilder.create(Object.class).build())
                .addParameter(MethodParameterBuilder.create(Object.class)
                    .addAnnotation(AnnotationDescriptionBuilder.create(MyAnnotation.class).build())
                    .addAnnotation(
                        AnnotationDescriptionBuilder.create(MyAnnotation2.class).withProperty("value", "foo").build())
                    .build())
                .build())
            .buildJava();

        final Class<?> interfaceClass = defineClass(expectedName, generator.byteCode());
        assertInterfaceDescription(expectedName, interfaceClass);

        assertEquals(1, interfaceClass.getDeclaredMethods().length);
        final Method method = interfaceClass.getMethod("doSomething", args2);

        assertEquals(3, method.getAnnotations().length);
        assertNotNull(method.getAnnotation(MyAnnotation.class));

        MyAnnotation2 annotation2 = method.getAnnotation(MyAnnotation2.class);
        assertEquals("foo", annotation2.value());
        assertEquals("", annotation2.field());

        MyAnnotation3 annotation3 = method.getAnnotation(MyAnnotation3.class);
        assertEquals("foo", annotation3.value());
        assertEquals("bar", annotation3.field());

        assertEquals(0, method.getParameters()[0].getAnnotations().length);
        assertEquals(2, method.getParameters()[1].getAnnotations().length);
        assertNotNull(method.getParameters()[1].getAnnotation(MyAnnotation.class));

        MyAnnotation2 paramAnno2 = method.getParameters()[1].getAnnotation(MyAnnotation2.class);
        assertEquals("foo", paramAnno2.value());
        assertEquals("", paramAnno2.field());
    }

    private static void assertInterfaceDescription(String expectedName, Class<?> interfaceClass) {
        assertNotNull(interfaceClass);
        assertTrue(interfaceClass.isInterface());
        assertTrue("Interface must be public", (interfaceClass.getModifiers() & Modifier.PUBLIC) != 0);
        assertEquals(expectedName, interfaceClass.getName());
    }

    private static Class<?> defineClass(String name,
            byte[] bytes) throws IllegalAccessException, ClassNotFoundException, InvocationTargetException {
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader newClassLoader = new OpenLClassLoader(oldClassLoader);
            Thread.currentThread().setContextClassLoader(newClassLoader);
            return ClassUtils.defineClass(name, bytes, newClassLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @SafeVarargs
    private final <T> T[] toArray(T... args) {
        return args;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.METHOD })
    public @interface MyAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.METHOD })
    public @interface MyAnnotation2 {
        String value();

        String field() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.METHOD })
    public @interface MyAnnotation3 {
        String value();

        String field();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.METHOD })
    public @interface MyAnnotation4 {
        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.METHOD })
    public @interface MyAnnotation5 {
        Class<?> value();
    }
}
