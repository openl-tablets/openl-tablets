package org.openl.itest;

import org.testcontainers.containers.OracleContainer;

@DisableOnArm
public class OracleSmokeTest extends RdbmsAbstractTest<OracleContainer> {

    @Override
    protected OracleContainer createJdbcDatabaseContainer() {
        return new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
                .withDatabaseName("studio")
                .withUsername("studio")
                .withPassword("studio");
    }
}
