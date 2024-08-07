package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class RunWebservicesITest {

    @Test
    public void test() throws Exception {
//        JettyServer.test();
        JettyServer.start().client().send("EPBDS-9519/EPBDS-9519_2_openapi");
    }

}
