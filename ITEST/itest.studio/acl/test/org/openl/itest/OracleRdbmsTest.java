package org.openl.itest;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.oracle.OracleContainer;

/**
 * Verifies the database upgrade against Oracle.
 *
 * @author Yury Molchan
 */
@CiWithDocker
class OracleRdbmsTest extends AbstractRdbmsTest {

    /**
     * Raises the process limit of the database, which runs every connection in a process of its own.
     *
     * <p>The image allows 200 processes. The database takes about 80 of them for itself, and the pool of the security
     * database keeps 50 more open. The design repository opens a connection for every call it makes, so a burst of
     * requests uses up the rest, and the listener refuses new connections with {@code ORA-12516} until it counts them
     * again.
     *
     * <p>The limit takes effect only after a restart, which the script performs before the container reports the
     * database ready.
     */
    private static final String RAISE_PROCESS_LIMIT = """
            ALTER SYSTEM SET processes = 1000 SCOPE = SPFILE;
            SHUTDOWN IMMEDIATE;
            STARTUP;
            """;

    @Override
    JdbcDatabaseContainer<?> createContainer() {
        return new OracleContainer("gvenzl/oracle-free:slim-faststart")
                .withCopyToContainer(Transferable.of(RAISE_PROCESS_LIMIT),
                        "/container-entrypoint-initdb.d/raise-process-limit.sql");
    }
}
