package org.openl.itest;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresSmokeTest extends RdbmsAbstractTest<PostgreSQLContainer> {

    @Override
    protected PostgreSQLContainer createJdbcDatabaseContainer() {
        return new PostgreSQLContainer("postgres:17.2")
                .withDatabaseName("studio")
                .withUsername("studio")
                .withPassword("studio");
    }

}
