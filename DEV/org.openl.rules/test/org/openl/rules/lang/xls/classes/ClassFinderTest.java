package org.openl.rules.lang.xls.classes;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import org.junit.Test;
import org.openl.rules.lang.xls.classes.test.TestBean;

public class ClassFinderTest {

    @Test
    public void testGetClasses() throws Exception {
        URL dirUrl = getClass().getResource("./test");
        URL jarUrl = new URL("jar:file:test/rules/classes/test.jar!/");
        URL exampleUrl = new URL("http://www.example.com"); // not used protocol
        String packageName = getClass().getPackage().getName();

        ClassFinder finder = new ClassFinder();

        ClassLoader stub = createClassLoaderStub(dirUrl);
        Class<?>[] classes = finder.getClasses(packageName + ".test", stub);
        assertEquals(1, classes.length);
        assertEquals(TestBean.class, classes[0]);

        stub = createClassLoaderStub(jarUrl);
        classes = finder.getClasses(packageName + ".test", stub);
        assertEquals(1, classes.length);

        stub = createClassLoaderStub(jarUrl, dirUrl);
        classes = finder.getClasses(packageName + ".test", stub);
        assertEquals(1, classes.length); // unique classes checking

        stub = createClassLoaderStub(exampleUrl);
        classes = finder.getClasses(packageName + ".test", stub);
        assertEquals(0, classes.length);
    }

    private ClassLoader createClassLoaderStub(final URL... urls) {
        return new ClassLoader() {

            @Override
            public Enumeration<URL> getResources(String name) {
                return Collections.enumeration(Arrays.asList(urls));
            }

        };
    }
}
