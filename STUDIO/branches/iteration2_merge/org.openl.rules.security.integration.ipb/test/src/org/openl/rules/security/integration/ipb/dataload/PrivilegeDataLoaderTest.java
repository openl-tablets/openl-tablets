package org.openl.rules.security.integration.ipb.dataload;

import junit.framework.TestCase;
import junit.framework.Assert;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Aliaksandr Antonik.
 */
public class PrivilegeDataLoaderTest extends TestCase {
    static class A {
        public static final String PRIVILEGE_OK1 = "ok1";
        public static final String PRIVILEGE_OK2 = "p_ok2";
        public final String PRIVILEGE_BAD1 = "not static";
        public static String PRIVILEGE_BAD2 = "not final"; 
        static final String PRIVILEGE_BAD3 = "not public";
        public static final Integer PRIVILEGE_BAD4 = "not String".hashCode();
        public static final String PRIVILEGE_BAD5 = null; // null
        public static final String PRIVILEGE_BAD6 = " "; // empty
        public static final String ROLE_BAD7 = "not privilege "; // empty
    }

    public void testEnumPrivileges() {
        final Set<String> expected = new HashSet<String>() {{
            add("ok1");
            add("p_ok2");
        }};

        List<String> ret = PrivilegeDataLoader.enumPrivileges(A.class);
        Assert.assertEquals("privilege set is wrong", new HashSet<String>(ret), expected);
    }
    
}
