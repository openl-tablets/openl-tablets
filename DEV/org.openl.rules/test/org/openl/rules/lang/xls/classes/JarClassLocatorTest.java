package org.openl.rules.lang.xls.classes;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.openl.rules.lang.xls.classes.test.TestBean;

public class JarClassLocatorTest {
    @Test
    public void testGetClasses() throws Exception {
        final List<Throwable> exceptions = new ArrayList<>();

        JarClassLocator locator = new JarClassLocator();
        locator.addExceptionHandler(new AbstractLocatorExceptionHandler() {
            @Override
            public void handleClassInstatiateException(Throwable t) {
                exceptions.add(t);
            }

        });

        String packageName = getClass().getPackage().getName();
        ClassLoader classLoader = getClass().getClassLoader();

        URL url = new URL("jar:file:test/rules/classes/test.jar!/");
        Collection<Class<?>> classes = locator.getClasses(url, packageName + ".test", classLoader);
        assertEquals(1, classes.size());
        assertEquals(TestBean.class, classes.iterator().next());
        assertEquals(1, exceptions.size());

        url = new URL("jar:file:test/not_exist.jar!/");
        classes = locator.getClasses(url, packageName + ".test", classLoader);
        assertEquals(0, classes.size());
        assertEquals(1, exceptions.size());
    }
}
