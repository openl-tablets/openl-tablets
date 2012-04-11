package org.openl.rules.util.net;

import junit.framework.TestCase;

import org.openl.rules.util.net.NetUtils;

public class NetUtilsTestCase extends TestCase {
    public void testIsLoopbackAddressInvalidParams() {
        assertFalse(NetUtils.isLoopbackAddress(""));
        assertFalse(NetUtils.isLoopbackAddress(null));
    }

    public void testIsLoopbackAddressV4() {
        assertTrue(NetUtils.isLoopbackAddress("127.0.0.1"));
        assertFalse(NetUtils.isLoopbackAddress("192.168.0.2"));
    }

    public void testIsLoopbackAddressV6() {
        assertTrue(NetUtils.isLoopbackAddress("::1"));
        assertTrue(NetUtils.isLoopbackAddress("0:0:0:0:0:0:0:1"));
        assertFalse(NetUtils.isLoopbackAddress("1080:0:0:0:8:800:200C:417A"));
    }
}
