package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class StringPoolTest {

    @AfterEach
    public void cleanUp() {
        StringPool.STRING_POOL.clear();
    }

    @Test
    public void testEmpty() {
        assertTrue(StringPool.STRING_POOL.isEmpty(), "String pool is not empty on before usage");
    }

    @Test
    public void testFirstIntern() {
        String str = "FirstIntern";
        String res = StringPool.intern(str);

        assertSame(str, res, "The returned string has not the same reference");
        assertTrue(StringPool.STRING_POOL.containsKey(str), "String pool has wrong key");
        assertEquals(1, StringPool.STRING_POOL.size(), "String pool has wrong size");
    }

    @Test
    public void testSecondIntern() {
        String str1 = new String("intern");
        String res1 = StringPool.intern(str1);
        String str2 = new String("intern");
        String res2 = StringPool.intern(str2);

        assertNotSame(str1, str2, "The test params has the same reference");
        assertSame(str1, res1, "The returned string has not the same reference");
        assertSame(res1, res2, "The returned string has not the same reference");
        assertTrue(StringPool.STRING_POOL.containsKey("intern"), "String pool has wrong key");
        assertEquals(1, StringPool.STRING_POOL.size(), "String pool has wrong size");
    }

    @Test
    public void testPoolMemory() {
        StringPool.intern("intern1");
        StringPool.intern("intern2");
        assertFalse(StringPool.STRING_POOL.containsKey("intern"), "String pool has wrong key");
        assertTrue(StringPool.STRING_POOL.containsKey("intern1"), "String pool has wrong key");
        assertTrue(StringPool.STRING_POOL.containsKey("intern2"), "String pool has wrong key");
        assertEquals(2, StringPool.STRING_POOL.size(), "String pool has wrong size");
    }

    @Test
    public void testGC() throws InterruptedException {
        StringPool.intern(new String("intern1"));
        String str = new String("intern2"); // Strong Reference
        StringPool.intern(str);
        StringPool.intern(new String("intern3"));
        System.gc();
        assertFalse(StringPool.STRING_POOL.containsKey("intern1"), "String pool has garbage collected string");
        assertTrue(StringPool.STRING_POOL.containsKey("intern2"), "String pool has not strong referenced string");
        assertFalse(StringPool.STRING_POOL.containsKey("intern3"), "String pool has garbage collected string");
        str.isEmpty(); // Strong Reference for IBM's JDK
    }
}
