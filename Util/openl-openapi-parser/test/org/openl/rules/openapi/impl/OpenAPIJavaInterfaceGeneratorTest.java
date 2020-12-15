package org.openl.rules.openapi.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.junit.Test;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.model.scaffolding.GeneratedJavaInterface;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.ClassUtils;

public class OpenAPIJavaInterfaceGeneratorTest {

    @Test
    public void testOpenAPIPathInfo() throws Throwable {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/slashProblem.json");

        GeneratedJavaInterface javaInterface = new OpenAPIJavaInterfaceGenerator(projectModel).generate();
        assertNotNull(javaInterface);

        Class<?> interfaceClass = defineClass(javaInterface.getJavaNameWithPackage(), javaInterface.getByteCode());
        assertInterfaceDescription(javaInterface.getJavaNameWithPackage(), interfaceClass);

        assertEquals(2, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("apiTodo", Integer.class);
        assertEquals(Integer.class, method1.getReturnType());

        assertEquals(4, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));

        Path method1Path = method1.getAnnotation(Path.class);
        assertEquals("/api/Todo", method1Path.value());

        Consumes method1Consumes = method1.getAnnotation(Consumes.class);
        assertArrayEquals(new String[]{"text/csv"}, method1Consumes.value());

        Produces method1Produces = method1.getAnnotation(Produces.class);
        assertArrayEquals(new String[]{"text/html"}, method1Produces.value());

        Method method2 = interfaceClass.getDeclaredMethod("apiBla", Integer.class);
        assertEquals(SpreadsheetResult.class, method2.getReturnType());

        assertEquals(4, method2.getDeclaredAnnotations().length);
        assertNotNull(method2.getAnnotation(POST.class));

        Path method2Path = method2.getAnnotation(Path.class);
        assertEquals("/api/Bla", method2Path.value());

        Consumes method2Consumes = method2.getAnnotation(Consumes.class);
        assertArrayEquals(new String[]{"application/json"}, method2Consumes.value());

        Produces method2Produces = method2.getAnnotation(Produces.class);
        assertArrayEquals(new String[]{"text/plain"}, method2Produces.value());
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
            ClassLoader newClassLoader = new OpenLBundleClassLoader(oldClassLoader);
            Thread.currentThread().setContextClassLoader(newClassLoader);
            return ClassUtils.defineClass(name, bytes, newClassLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void writeToFile(byte[] byteCode) throws IOException {
        try (FileOutputStream fos = new FileOutputStream("OpenAPIService.class")) {
            fos.write(byteCode);
        }
    }
}
