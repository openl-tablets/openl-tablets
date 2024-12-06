package org.openl.itest;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.openl.itest.core.JettyServer;

@SuppressWarnings("rawtypes")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class RdbmsAbstractTest<T extends JdbcDatabaseContainer> {

    private T jdbcDatabaseContainer;
    private Map<String, String> config;

    @BeforeAll
    void initialize() {
        jdbcDatabaseContainer = createJdbcDatabaseContainer();
        jdbcDatabaseContainer.start();

        config = Map.of(
                "db.url", jdbcDatabaseContainer.getJdbcUrl(),
                "db.user", jdbcDatabaseContainer.getUsername(),
                "db.password", jdbcDatabaseContainer.getPassword()
        );
    }

    @AfterAll
    void destroy() {
        if (jdbcDatabaseContainer != null) {
            jdbcDatabaseContainer.stop();
        }
    }

    @Test
    void smoke() throws Exception {
        JettyServer server = null;
        try {
            server = JettyServer.start("smoke", config);
            var httpClient = server.client();
            httpClient.test("test-resources-smoke");
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    protected abstract T createJdbcDatabaseContainer();

}
