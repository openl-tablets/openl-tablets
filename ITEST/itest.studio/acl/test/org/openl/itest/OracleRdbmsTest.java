package org.openl.itest;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.oracle.OracleContainer;

/**
 * Verifies the database upgrade against Oracle.
 *
 * @author Yury Molchan
 */
@CiWithDocker
class OracleRdbmsTest extends AbstractRdbmsTest {

    @Override
    JdbcDatabaseContainer<?> createContainer() {
        return new OracleContainer("gvenzl/oracle-free:slim-faststart");
    }
}
