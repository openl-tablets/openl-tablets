package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class RunUnpackClasspathZipTest {

    @Test
    void testClasspath() throws Exception {
        JettyServer.test("classpath");
    }

    @Test
    void testDeployToRemoteRepoFromClasspath() throws Exception {
        JettyServer.test("remote");
    }

}
