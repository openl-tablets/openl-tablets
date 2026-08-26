package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class MultipartLimitTest {

    private static final String MAX_FORM_KEYS_PROPERTY = "org.eclipse.jetty.server.Request.maxFormKeys";

    @Test
    void multipartPartLimitReturnsPayloadTooLarge() throws Exception {
        var previousLimit = System.getProperty(MAX_FORM_KEYS_PROPERTY);
        try {
            System.setProperty(MAX_FORM_KEYS_PROPERTY, "3");
            try (var client = JettyServer.get().start()) {
                client.localEnv.put("ADMIN_AUTH_TOCKEN", "Basic YWRtaW46YWRtaW4=");
                client.test("test-resources-multipart-limit");
            }
        } finally {
            if (previousLimit == null) {
                System.clearProperty(MAX_FORM_KEYS_PROPERTY);
            } else {
                System.setProperty(MAX_FORM_KEYS_PROPERTY, previousLimit);
            }
        }
    }
}
