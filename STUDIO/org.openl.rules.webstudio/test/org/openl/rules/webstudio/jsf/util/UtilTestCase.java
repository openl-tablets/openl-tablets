package org.openl.rules.webstudio.jsf.util;

import org.openl.rules.webstudio.util.Util;

import junit.framework.TestCase;

public class UtilTestCase extends TestCase {
    public void testIsLoopbackAddressV4() {
        assertTrue(Util.isLoopbackAddress("127.0.0.1"));
        assertFalse(Util.isLoopbackAddress("192.168.0.2"));
    }

    public void testIsLoopbackAddressV6() {
        assertTrue(Util.isLoopbackAddress("::1"));
        assertTrue(Util.isLoopbackAddress("0:0:0:0:0:0:0:1"));
        assertFalse(Util.isLoopbackAddress("1080:0:0:0:8:800:200C:417A"));
    }

    public void testIsLoopbackAddressInvalidParams() {
        assertFalse(Util.isLoopbackAddress(""));
        assertFalse(Util.isLoopbackAddress(null));
    }
}
