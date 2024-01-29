package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class WebStudioTest {

    @Test
    public void wizard() throws Exception {
        JettyServer.test("wizard");
    }

    @Test
    public void simple() throws Exception {
        JettyServer.test("simple");
    }

    @Test
    public void multi() throws Exception {
        JettyServer.test("multi");
    }

    @Test
    public void repos() throws Exception {
        JettyServer.test("repos");
    }

    @Test
    public void dtr() throws Exception {
        JettyServer.test("dtr");
    }
}
