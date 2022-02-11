package org.openl.itest;

import org.junit.Test;
import org.openl.itest.core.JettyServer;

public class RunITest {

    @Test
    public void test() throws Exception {
        JettyServer.test();
    }
}
