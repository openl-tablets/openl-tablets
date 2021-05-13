package org.openl.rules.ruleservice.storelogdata.hive;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HiveDataSource implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(HiveDataSource.class);
    private static final String driverClass = "org.apache.hive.jdbc.HiveDriver";

    private static final HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;
    private boolean enabled;
    private String connectionURL;
    private String username;
    private String password;

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //todo hikari config file
        if (enabled) {
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException exception) {
                log.error("The driver is not found", exception);
            }
            config.setJdbcUrl(connectionURL);
            config.setUsername(username);
            config.setPassword(password);
            ds = new HikariDataSource(config);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void close(){
        ds.close();
    }

}
