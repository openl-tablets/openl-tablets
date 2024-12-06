package org.openl.itest;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("rawtypes")
public class MssqlSmokeTest extends RdbmsAbstractTest<MSSQLServerContainer> {

    @Override
    protected MSSQLServerContainer createJdbcDatabaseContainer() {
        return (MSSQLServerContainer) new AzureSqlEdgeContainerProvider().newInstance();
    }

    /**
     * Uses a {@link MSSQLServerContainer} with a `mcr.microsoft.com/azure-sql-edge` image (default: "latest") in place of
     * the standard ` mcr.microsoft.com/mssql/server` image
     */
    public static class AzureSqlEdgeContainerProvider extends JdbcDatabaseContainerProvider {
        private static final String NAME = "azuresqledge";

        @Override
        public boolean supports(String databaseType) {
            return databaseType.equals(NAME);
        }

        @Override
        public JdbcDatabaseContainer newInstance() {
            return newInstance("latest");
        }

        @Override
        public JdbcDatabaseContainer newInstance(String tag) {
            var taggedImageName = DockerImageName.parse("mcr.microsoft.com/azure-sql-edge")
                    .withTag(tag)
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"); // From MSSQLServerContainer.DEFAULT_IMAGE_NAME
            return new MSSQLServerContainer(taggedImageName)
                    .withUrlParam("encrypt", "false");
        }
    }

}
