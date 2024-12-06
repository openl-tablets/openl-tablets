package org.openl.itest;

import org.testcontainers.containers.MySQLContainer;

@SuppressWarnings("rawtypes")
public class MysqlSmokeTest extends RdbmsAbstractTest<MySQLContainer> {

    @Override
    protected MySQLContainer createJdbcDatabaseContainer() {
        return new MySQLContainer("mysql:9.1.0")
                .withDatabaseName("studio")
                .withUsername("studio")
                .withPassword("studio");
    }
}
