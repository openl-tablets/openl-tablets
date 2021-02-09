package org.openl.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.classloader.OpenLClassLoader;

public class UserContextTest {

    @Test
    public void testEqualityForDifferentClassloaders() {
        ClassLoader classLoader = UserContextTest.class.getClassLoader();
        ClassLoader cl1 = new OpenLClassLoader(classLoader);

        UserContext uc1 = new UserContext(cl1, ".");
        UserContext uc2 = new UserContext(cl1, ".");
        assertEquals(uc1, uc2);
        assertEquals(uc1.hashCode(), uc2.hashCode());

        ClassLoader cl2 = new OpenLClassLoader(classLoader);
        uc2 = new UserContext(cl2, ".");
        assertNotEquals(uc1, uc2);
        assertNotEquals(uc1.hashCode(), uc2.hashCode());
    }
}
