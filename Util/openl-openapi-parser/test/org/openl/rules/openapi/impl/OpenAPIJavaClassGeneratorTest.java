package org.openl.rules.openapi.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import org.junit.Before;
import org.junit.Test;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.util.ClassUtils;

public class OpenAPIJavaClassGeneratorTest {

    private OpenAPIModelConverter converter;
    private ClassLoader newClassLoader;

    @Before
    public void setUp() {
        converter = new OpenAPIScaffoldingConverter();
        newClassLoader = new OpenLBundleClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testOpenAPIPathInfo() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/slashProblem.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();

        JavaClassFile javaInterface = generated.getAnnotationTemplateClass();
        Class<?> interfaceClass = defineClass(javaInterface.getJavaNameWithPackage(), javaInterface.getByteCode());
        assertInterfaceDescription(javaInterface.getJavaNameWithPackage(), interfaceClass);

        assertEquals(2, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("apiTodo", Integer.class);
        assertEquals(Integer.class, method1.getReturnType());

        assertEquals(4, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/api/Todo", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[]{"text/csv"}, method1.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[]{"text/html"}, method1.getAnnotation(Produces.class).value());
        assertEquals(0, method1.getParameters()[0].getDeclaredAnnotations().length);

        Method method2 = interfaceClass.getDeclaredMethod("apiBla", Integer.class);
        assertEquals(SpreadsheetResult.class, method2.getReturnType());

        assertEquals(4, method2.getDeclaredAnnotations().length);
        assertNotNull(method2.getAnnotation(POST.class));
        assertEquals("/api/Bla", method2.getAnnotation(Path.class).value());
        assertArrayEquals(new String[]{"application/json"}, method2.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[]{"text/plain"}, method2.getAnnotation(Produces.class).value());
        assertEquals(0, method2.getParameters()[0].getDeclaredAnnotations().length);
    }

    @Test
    public void testOpenAPIJavaInterfaceGenerator() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/openapi.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        JavaClassFile javaInterface = generated.getAnnotationTemplateClass();

        Class<?> interfaceClass = defineClass(javaInterface.getJavaNameWithPackage(), javaInterface.getByteCode());
        assertInterfaceDescription(javaInterface.getJavaNameWithPackage(), interfaceClass);

        assertEquals(4, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("apipolicyProxy2", Object[].class);
        assertEquals(Object[].class, method1.getReturnType());
        assertEquals(5, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/api/policyProxy2", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[]{"application/json"}, method1.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[]{"application/json"}, method1.getAnnotation(Produces.class).value());
        assertEquals("Policy", method1.getAnnotation(RulesType.class).value());

        assertEquals(1, method1.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method1.getParameters()[0].getAnnotation(RulesType.class).value());

        Method method2 = interfaceClass.getDeclaredMethod("apipolicyProxy3", Object.class, Object.class);
        assertEquals(Object[].class, method2.getReturnType());
        assertEquals(5, method2.getDeclaredAnnotations().length);
        assertNotNull(method2.getAnnotation(POST.class));
        assertEquals("/api/policyProxy3", method2.getAnnotation(Path.class).value());
        assertArrayEquals(new String[]{"application/json"}, method2.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[]{"application/json"}, method2.getAnnotation(Produces.class).value());
        assertEquals("Policy", method2.getAnnotation(RulesType.class).value());

        assertEquals(1, method2.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method2.getParameters()[0].getAnnotation(RulesType.class).value());
        assertEquals(1, method2.getParameters()[1].getAnnotations().length);
        assertEquals("Policy", method2.getParameters()[1].getAnnotation(RulesType.class).value());

        Method method3 = interfaceClass.getDeclaredMethod("apipolicyProxy", Object.class);
        assertEquals(Object.class, method3.getReturnType());
        assertEquals(5, method3.getDeclaredAnnotations().length);
        assertNotNull(method3.getAnnotation(POST.class));
        assertEquals("/api/policyProxy", method3.getAnnotation(Path.class).value());
        assertArrayEquals(new String[]{"application/json"}, method3.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[]{"application/json"}, method3.getAnnotation(Produces.class).value());
        assertEquals("Policy", method3.getAnnotation(RulesType.class).value());

        assertEquals(1, method3.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method3.getParameters()[0].getAnnotation(RulesType.class).value());

        Method method4 = interfaceClass.getDeclaredMethod("apidoSomething", Object.class);
        assertEquals(SpreadsheetResult.class, method4.getReturnType());
        assertEquals(4, method4.getDeclaredAnnotations().length);
        assertNotNull(method4.getAnnotation(POST.class));
        assertEquals("/api/doSomething", method4.getAnnotation(Path.class).value());
        assertArrayEquals(new String[]{"application/json"}, method4.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[]{"application/json"}, method4.getAnnotation(Produces.class).value());

        assertEquals(1, method4.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method4.getParameters()[0].getAnnotation(RulesType.class).value());
    }

    @Test
    public void testOpenAPIJavaInterfaceGeneratorRuntimeContext() throws Exception {
        ProjectModel projectModel = converter
                .extractProjectModel("test.converter/paths/runtimeContextAndExtraMethod.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();

        for (JavaClassFile javaClass : generated.getCommonClasses()) {
            Class<?> clazz = defineClass(javaClass.getJavaNameWithPackage(), javaClass.getByteCode());
            assertFalse(clazz.isInterface());
            assertTrue("Class must be public", (clazz.getModifiers() & Modifier.PUBLIC) != 0);
            assertEquals(javaClass.getJavaNameWithPackage(), clazz.getName());
            clazz.newInstance();//just for sure
        }
        JavaClassFile javaInterface = generated.getAnnotationTemplateClass();
        Class<?> interfaceClass = defineClass(javaInterface.getJavaNameWithPackage(), javaInterface.getByteCode());
        assertInterfaceDescription(javaInterface.getJavaNameWithPackage(), interfaceClass);

        //assertEquals(2, interfaceClass.getDeclaredMethods().length);

//        Method method1 = interfaceClass.getDeclaredMethod("apiTodo", Object[].class);
//        assertEquals(Object[].class, method1.getReturnType());
//        assertEquals(5, method1.getDeclaredAnnotations().length);
//        assertNotNull(method1.getAnnotation(POST.class));
//        assertEquals("/api/policyProxy2", method1.getAnnotation(Path.class).value());
//        assertArrayEquals(new String[]{"application/json"}, method1.getAnnotation(Consumes.class).value());
//        assertArrayEquals(new String[]{"application/json"}, method1.getAnnotation(Produces.class).value());
//        assertEquals("Policy", method1.getAnnotation(RulesType.class).value());
    }

    @Test
    public void resolveTypeTest() {
        TypeInfo typeInfo = new TypeInfo("Policy", true);
        assertEquals(Object.class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));

        typeInfo.setDimension(1);
        assertEquals(Object[].class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));

        typeInfo.setDimension(3);
        assertEquals(Object[][][].class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));

        typeInfo = new TypeInfo(Integer.class.getName(), Integer.class.getSimpleName());
        assertEquals(Integer.class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));
    }

    private static void assertInterfaceDescription(String expectedName, Class<?> interfaceClass) {
        assertNotNull(interfaceClass);
        assertTrue(interfaceClass.isInterface());
        assertTrue("Interface must be public", (interfaceClass.getModifiers() & Modifier.PUBLIC) != 0);
        assertEquals(expectedName, interfaceClass.getName());
    }

    private Class<?> defineClass(String name,
            byte[] bytes) throws IllegalAccessException, ClassNotFoundException, InvocationTargetException {
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
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
