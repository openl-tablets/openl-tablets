package org.openl.info;

import java.net.URL;
import java.sql.DriverManager;

/**
 * A utility class to display all JDBC drivers on the classpath.
 *
 * @author Yury Molchan
 */
final class JdbcLogger extends OpenLLogger {
    @Override
    protected String getName() {
        return "jdbc";
    }

    @Override
    protected void discover() {
        log("Recognized JDBC drivers:");
        var drivers = DriverManager.getDrivers();

        if (!drivers.hasMoreElements()) {
            log("    No JDBC drivers were found on the Java classpath.");
            return;
        }

        int i = 1;
        // Iterate through the list of drivers and print their details
        while (drivers.hasMoreElements()) {
            var driver = drivers.nextElement();
            log("  {}.  {}    v{}.{}    @ {}",
                    String.valueOf(i++),
                    driver.getClass().getName(),
                    String.valueOf(driver.getMajorVersion()),
                    String.valueOf(driver.getMinorVersion()),
                    getPath(driver.getClass()));
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
}
