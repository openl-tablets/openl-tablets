package org.openl.rules.webstudio.web.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

public class LaunchFileServletTest {

    @Test
    public void testIsLoopbackAddressInvalidParams() {
        assertFalse(LaunchFileServlet.isLoopbackAddress(""));
        assertFalse(LaunchFileServlet.isLoopbackAddress(null));
    }

    @Test
    public void testIsLoopbackAddressV4() {
        assertTrue(LaunchFileServlet.isLoopbackAddress("127.0.0.1"));
        assertFalse(LaunchFileServlet.isLoopbackAddress("192.168.0.2"));
    }

    @Test
    public void testIsLoopbackAddressV6() {
        assertTrue(LaunchFileServlet.isLoopbackAddress("::1"));
        assertTrue(LaunchFileServlet.isLoopbackAddress("0:0:0:0:0:0:0:1"));
        assertFalse(LaunchFileServlet.isLoopbackAddress("1080:0:0:0:8:800:200C:417A"));
    }

}
