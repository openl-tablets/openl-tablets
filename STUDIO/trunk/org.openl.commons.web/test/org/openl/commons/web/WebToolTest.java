package org.openl.commons.web;

import org.openl.commons.web.util.WebTool;

import junit.framework.TestCase;

public class WebToolTest extends TestCase {

    public void testIsLoopbackAddressInvalidParams() {
        assertFalse(WebTool.isLoopbackAddress(""));
        assertFalse(WebTool.isLoopbackAddress(null));
    }

    public void testIsLoopbackAddressV4() {
        assertTrue(WebTool.isLoopbackAddress("127.0.0.1"));
        assertFalse(WebTool.isLoopbackAddress("192.168.0.2"));
    }

    public void testIsLoopbackAddressV6() {
        assertTrue(WebTool.isLoopbackAddress("::1"));
        assertTrue(WebTool.isLoopbackAddress("0:0:0:0:0:0:0:1"));
        assertFalse(WebTool.isLoopbackAddress("1080:0:0:0:8:800:200C:417A"));
    }

}
