package org.openl.util.db;

import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class which register all supported drivers in the classloader.
 *
 * @author Yury Molchan
 */
public class JDBCDriverRegister {

    /**
     * Register JDBC drivers in the classloader
     *
     * @see java.sql.DriverManager#loadInitialDrivers()
     */
    public static void registerDrivers() {
        // Loads drivers from jdbc.drivers system property like in DriverManager
        String[] drivers = StringUtils.split(System.getProperty("jdbc.drivers"), ':');
        registerDrivers(drivers);

        // Defaults drivers
        registerDrivers("com.mysql.jdbc.Driver",
            "org.mariadb.jdbc.Driver",
            "com.ibm.db2.jcc.DB2Driver",
            "oracle.jdbc.OracleDriver",
            "org.postgresql.Driver",
            "org.hsqldb.jdbcDriver",
            "org.h2.Driver",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    private static void registerDrivers(String... drivers) {
        if (drivers == null) {
            return;
        }
        Logger log = LoggerFactory.getLogger(JDBCDriverRegister.class);
        for (String driver : drivers) {
            try {
                Class.forName(driver);
                log.info("JDBC Driver: '{}' - OK.", driver);
            } catch (ClassNotFoundException e) {
                log.info("JDBC Driver: '{}' - NOT FOUND.", driver);
            }
        }
    }

    /**
     * For Spring initialization purposes.
     */
    public void init() {
        registerDrivers();
    }
}
