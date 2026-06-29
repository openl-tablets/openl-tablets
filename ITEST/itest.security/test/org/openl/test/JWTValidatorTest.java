package org.openl.test;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class JWTValidatorTest {

    @Test
    void test() throws Exception {
        JettyServer.test("jwt");
    }
}
