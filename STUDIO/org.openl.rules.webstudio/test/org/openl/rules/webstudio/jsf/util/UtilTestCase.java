package org.openl.rules.webstudio.jsf.util;

import org.openl.rules.webstudio.web.jsf.util.WebStudioUtils;

import junit.framework.TestCase;

public class UtilTestCase extends TestCase {
    public void testIsLoopbackAddressV4() {
        assertTrue(WebStudioUtils.isLoopbackAddress("127.0.0.1"));
        assertFalse(WebStudioUtils.isLoopbackAddress("192.168.0.2"));
    }

    public void testIsLoopbackAddressV6() {
        assertTrue(WebStudioUtils.isLoopbackAddress("::1"));
        assertTrue(WebStudioUtils.isLoopbackAddress("0:0:0:0:0:0:0:1"));
        assertFalse(WebStudioUtils.isLoopbackAddress("1080:0:0:0:8:800:200C:417A"));
    }

    public void testIsLoopbackAddressInvalidParams() {
        assertFalse(WebStudioUtils.isLoopbackAddress(""));
        assertFalse(WebStudioUtils.isLoopbackAddress(null));
    }
}
