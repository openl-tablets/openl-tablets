package org.openl.conf;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.classloader.SimpleBundleClassLoader;

public class UserContextTest {

    @Test
    public void testEqualityForDifferentClassloaders() {
        ClassLoader classLoader = UserContextTest.class.getClassLoader();
        ClassLoader cl1 = new SimpleBundleClassLoader(classLoader);

        UserContext uc1 = new UserContext(cl1, ".");
        UserContext uc2 = new UserContext(cl1, ".");
        assertEquals(uc1, uc2);
        assertEquals(uc1.hashCode(), uc2.hashCode());

        ClassLoader cl2 = new SimpleBundleClassLoader(classLoader);
        uc2 = new UserContext(cl2, ".");
        assertFalse(uc1.equals(uc2));
        assertFalse(uc1.hashCode() == uc2.hashCode());
    }
}
