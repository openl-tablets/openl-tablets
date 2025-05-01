package org.openl.itest;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import org.openl.itest.core.JettyServer;

@DisabledIfSystemProperty(named = "noDocker", matches = ".*")
public class RdbmsTest {

    @Test
    public void mysql() throws Exception {
        testDB(() -> new MySQLContainer("mysql:lts") {
            @Override
            public String getDriverClassName() {
                return "org.mariadb.jdbc.Driver";
            }
        });
    }

    /**
     * Note: <a href="https://learn.microsoft.com/en-us/azure/azure-sql-edge/features">Supported features of Azure SQL Edge</a>
     * 1. Azure SQL Edge will be retired on September 30, 2025.
     * 2. Azure SQL Edge no longer supports the ARM64 platform.
     */
    @Test
    public void sqlserver() throws Exception {
        testDB(() -> new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest")
                .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")).acceptLicense());
    }

    @Test
    public void oracle() throws Exception {
        testDB(() -> new OracleContainer("gvenzl/oracle-free:slim-faststart"));
    }

    @Test
    public void postgresql() throws Exception {
        testDB(() -> {
            var db = new PostgreSQLContainer("postgres:alpine");
            db.waitingFor(new HostPortWaitStrategy());
            return db;
        });
    }

    private static void testDB(Supplier<JdbcDatabaseContainer<?>> containerSupplier) throws Exception {
        try (var db = containerSupplier.get()) {
            db.start();
            JettyServer.get()
                    .withInitParam("db.url", db.getJdbcUrl())
                    .withInitParam("db.user", db.getUsername())
                    .withInitParam("db.password", db.getPassword())
                    .withProfile("acl")
                    .test();
        }
    }

}
