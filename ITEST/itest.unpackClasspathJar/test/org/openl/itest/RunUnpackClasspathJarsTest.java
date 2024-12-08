package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class RunUnpackClasspathJarsTest {

    @Test
    public void test() throws Exception {
        JettyServer.get().test();
    }

}
