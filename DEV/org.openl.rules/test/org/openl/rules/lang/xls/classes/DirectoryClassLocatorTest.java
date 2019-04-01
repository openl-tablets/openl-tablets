package org.openl.rules.lang.xls.classes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.openl.rules.lang.xls.classes.test.TestBean;

public class DirectoryClassLocatorTest {
    @Test
    public void testGetClasses() throws Exception {
        final List<Throwable> exceptions = new ArrayList<>();

        DirectoryClassLocator locator = new DirectoryClassLocator();
        locator.addExceptionHandler(new AbstractLocatorExceptionHandler() {
            @Override
            public void handleClassInstatiateException(Throwable t) {
                exceptions.add(t);
            }

            @Override
            public void handleURLParseException(Exception e) {
                exceptions.add(e);
            }

            @Override
            public void handleIOException(IOException e) {
                exceptions.add(e);
            }

        });

        String packageName = getClass().getPackage().getName();
        ClassLoader classLoader = getClass().getClassLoader();

        URL url = getClass().getResource("./test");
        Collection<Class<?>> classes = locator.getClasses(url, packageName + ".test", classLoader);
        assertEquals(1, classes.size());
        assertEquals(TestBean.class, classes.iterator().next());
        assertEquals(1, exceptions.size());

        exceptions.clear();
        url = getClass().getResource(".");
        classes = locator.getClasses(url, packageName, classLoader);
        assertTrue(ArrayUtils.contains(classes.toArray(), getClass()));
        assertEquals(0, exceptions.size());

        exceptions.clear();
        url = new URL("file:/not_exist");
        classes = locator.getClasses(url, packageName, classLoader);
        assertEquals(0, classes.size());
        assertEquals(0, exceptions.size());

        exceptions.clear();
        url = new URL("file:/<>?");
        classes = locator.getClasses(url, packageName, classLoader);
        assertEquals(0, classes.size());
        assertEquals(1, exceptions.size());
    }
}
