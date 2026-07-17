package org.openl.itest;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * Verifies the database upgrade against MySQL.
 *
 * @author Yury Molchan
 */
@CiWithDocker
class MysqlRdbmsTest extends AbstractRdbmsTest {

    @Override
    JdbcDatabaseContainer<?> createContainer() {
        // OpenL bundles the MariaDB driver, which also speaks the MySQL protocol.
        return new MySQLContainer("mysql:lts") {
            @Override
            public String getDriverClassName() {
                return "org.mariadb.jdbc.Driver";
            }
        };
    }
}
