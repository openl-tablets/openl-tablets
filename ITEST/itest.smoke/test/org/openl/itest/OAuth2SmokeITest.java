package org.openl.itest;

import org.junit.Test;
import org.openl.itest.core.JettyServer;

public class OAuth2SmokeITest {

    @Test
    public void test() throws Exception {
        JettyServer.test("oauth");
    }

}
