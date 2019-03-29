package org.openl.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

public class StringPoolTest {

    @After
    public void cleanUp() {
        StringPool.STRING_POOL.clear();
    }

    @Test
    public void testEmpty() {
        assertTrue("String pool is not empty on before usage", StringPool.STRING_POOL.isEmpty());
    }

    @Test
    public void testFirstIntern() {
        String str = "FirstIntern";
        String res = StringPool.intern(str);

        assertSame("The returned string has not the same reference", str, res);
        assertTrue("String pool has wrong key", StringPool.STRING_POOL.containsKey(str));
        assertEquals("String pool has wrong size", 1, StringPool.STRING_POOL.size());
    }

    @Test
    public void testSecondIntern() {
        String str1 = new String("intern");
        String res1 = StringPool.intern(str1);
        String str2 = new String("intern");
        String res2 = StringPool.intern(str2);

        assertNotSame("The test params has the same reference", str1, str2);
        assertSame("The returned string has not the same reference", str1, res1);
        assertSame("The returned string has not the same reference", res1, res2);
        assertTrue("String pool has wrong key", StringPool.STRING_POOL.containsKey("intern"));
        assertEquals("String pool has wrong size", 1, StringPool.STRING_POOL.size());
    }

    @Test
    public void testPoolMemory() {
        StringPool.intern("intern1");
        StringPool.intern("intern2");
        assertFalse("String pool has wrong key", StringPool.STRING_POOL.containsKey("intern"));
        assertTrue("String pool has wrong key", StringPool.STRING_POOL.containsKey("intern1"));
        assertTrue("String pool has wrong key", StringPool.STRING_POOL.containsKey("intern2"));
        assertEquals("String pool has wrong size", 2, StringPool.STRING_POOL.size());
    }

    @Test
    public void testGC() throws InterruptedException {
        StringPool.intern(new String("intern1"));
        String str = new String("intern2"); // Strong Reference
        StringPool.intern(str);
        StringPool.intern(new String("intern3"));
        System.gc();
        assertFalse("String pool has garbage collected string", StringPool.STRING_POOL.containsKey("intern1"));
        assertTrue("String pool has not strong referenced string", StringPool.STRING_POOL.containsKey("intern2"));
        assertFalse("String pool has garbage collected string", StringPool.STRING_POOL.containsKey("intern3"));
        str.isEmpty(); // Strong Reference for IBM's JDK
    }
}
