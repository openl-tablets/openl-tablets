package org.openl.itest;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the database upgrade against SQL Server.
 *
 * <p>Note: <a href="https://learn.microsoft.com/en-us/azure/azure-sql-edge/features">Supported features of Azure SQL Edge</a>
 * 1. Azure SQL Edge will be retired on September 30, 2025.
 * 2. Azure SQL Edge no longer supports the ARM64 platform.
 *
 * @author Yury Molchan
 */
@CiWithDocker
class SqlServerRdbmsTest extends AbstractRdbmsTest {

    @Override
    JdbcDatabaseContainer<?> createContainer() {
        return new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest")
                .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")).acceptLicense();
    }
}
