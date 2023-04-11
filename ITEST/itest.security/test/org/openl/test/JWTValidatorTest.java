package org.openl.test;

import org.junit.Test;
import org.openl.itest.core.JettyServer;

public class JWTValidatorTest {

    @Test
    public void test() throws Exception {
        JettyServer.test("jwt");
    }
}
