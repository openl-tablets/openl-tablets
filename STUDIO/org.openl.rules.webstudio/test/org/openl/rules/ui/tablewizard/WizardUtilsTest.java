package org.openl.rules.ui.tablewizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;

import org.openl.rules.ui.tablewizard.test.TestBean;

public class WizardUtilsTest {

    @Test
    public void testGetClasses() throws Exception {
        URL dirUrl = getClass().getResource("./test");
        URL jarUrl = new URL("jar:file:test/rules/classes/test.jar!/");
        var notExistJar = new URL("jar:file:test/not_exist.jar!/");
        URL exampleUrl = new URL("http://www.example.com"); // not used protocol
        String packageName = "org.openl.rules.lang.xls.classes";

        ClassLoader stub = createClassLoaderStub(dirUrl);
        var classes = WizardUtils.getClasses("org.openl.rules.ui.tablewizard.test", stub);
        assertEquals(1, classes.size());
        assertEquals(TestBean.class, classes.iterator().next());

        stub = createClassLoaderStub(jarUrl);
        classes = WizardUtils.getClasses(packageName + ".test", stub);
        assertEquals(1, classes.size());

        stub = createClassLoaderStub(notExistJar);
        classes = WizardUtils.getClasses(packageName + ".test", stub);
        assertEquals(0, classes.size());

        stub = createClassLoaderStub(jarUrl, dirUrl);
        classes = WizardUtils.getClasses(packageName + ".test", stub);
        assertEquals(1, classes.size()); // unique classes checking

        stub = createClassLoaderStub(exampleUrl);
        classes = WizardUtils.getClasses(packageName + ".test", stub);
        assertEquals(0, classes.size());

        var url = getClass().getResource(".");
        stub = createClassLoaderStub(url);
        classes = WizardUtils.getClasses("org.openl.rules.ui.tablewizard", stub);
        assertTrue(classes.contains(getClass()));

        url = new URL("file:/not_exist");
        stub = createClassLoaderStub(url);
        classes = WizardUtils.getClasses(packageName, stub);
        assertEquals(0, classes.size());

        url = new URL("file:/<>?");
        stub = createClassLoaderStub(url);
        classes = WizardUtils.getClasses(packageName, stub);
        assertEquals(0, classes.size());
    }

    private ClassLoader createClassLoaderStub(final URL... urls) {
        return new URLClassLoader(urls) {

            @Override
            public Enumeration<URL> getResources(String name) {
                return Collections.enumeration(Arrays.asList(urls));
            }

        };
    }
}
