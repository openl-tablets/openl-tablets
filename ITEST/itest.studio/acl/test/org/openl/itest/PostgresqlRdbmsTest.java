package org.openl.itest;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

/**
 * Verifies the database upgrade against PostgreSQL.
 *
 * @author Yury Molchan
 */
@CiWithDocker
class PostgresqlRdbmsTest extends AbstractRdbmsTest {

    @Override
    JdbcDatabaseContainer<?> createContainer() {
        var db = new PostgreSQLContainer("postgres:alpine");
        db.waitingFor(new HostPortWaitStrategy());
        return db;
    }
}
