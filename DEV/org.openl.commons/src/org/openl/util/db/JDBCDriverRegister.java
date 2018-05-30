package org.openl.util.db;

import java.net.URL;
import java.sql.Driver;

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
        registerDrivers("org.h2.Driver",
            "org.hsqldb.jdbcDriver",
            "org.postgresql.Driver",
            "org.mariadb.jdbc.Driver",
            "com.mysql.cj.jdbc.Driver",
            "com.mysql.jdbc.Driver",
            "com.ibm.db2.jcc.DB2Driver",
            "oracle.jdbc.OracleDriver",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    private static void registerDrivers(String... drivers) {
        if (drivers == null) {
            return;
        }
        Logger log = LoggerFactory.getLogger(JDBCDriverRegister.class);
        for (String driver : drivers) {
            Class<?> aClass;
            try {
                aClass = Class.forName(driver);
            } catch (ClassNotFoundException e) {
                log.info("JDBC Driver: '{}' - NOT FOUND.", driver);
                continue;
            }
            String path = getPath(aClass);
            try {
                Driver dr = Driver.class.cast(aClass.newInstance());
                int majorVersion = dr.getMajorVersion();
                int minorVersion = dr.getMinorVersion();

                log.info("JDBC Driver: '{}' - OK.\n      Path: {}\n      Version: {}.{}",
                    driver,
                    path,
                    majorVersion,
                    minorVersion);
            } catch (Exception e) {
                log.info("JDBC Driver: '{}' - ERROR.\n      Path: {}", driver, path, e);
            }
        }
    }

    private static String getPath(Class<?> aClass) {
        try {
            URL resource = aClass.getResource(aClass.getSimpleName() + ".class");
            if (resource != null) {
                return resource.toString();
            }
            return "UNKNOWN";
        } catch (Exception ignored) {
            return "UNKNOWN, because of an exception has been happened.";
        }
    }

    /**
     * For Spring initialization purposes.
     */
    public void init() {
        registerDrivers();
    }
}
