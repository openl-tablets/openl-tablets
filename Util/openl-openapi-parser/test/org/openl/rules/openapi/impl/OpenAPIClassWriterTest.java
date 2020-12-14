package org.openl.rules.openapi.impl;

import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.ClassUtils;

public class OpenAPIClassWriterTest {

    @Test
    public void testOpenAPIPathInfo() throws Throwable {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/slashProblem.json");

        OpenAPIClassWriter openAPIClassWriter = new OpenAPIClassWriter(projectModel);
        openAPIClassWriter.generateInterface();

        byte[] b = openAPIClassWriter.getByteCode();

        try (FileOutputStream fos = new FileOutputStream("OpenAPIService.class")) {
            fos.write(b);
        }

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
}
