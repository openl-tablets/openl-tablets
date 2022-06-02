package org.openl.itest;

import org.junit.Test;
import org.openl.itest.core.JettyServer;

public class RunUnpackClasspathZipTest {

    @Test
    public void testClasspath() throws Exception {
        JettyServer.test("classpath");
    }

    @Test
    public void testDeployToRemoteRepoFromClasspath() throws Exception {
        JettyServer.test("remote");
    }

}
