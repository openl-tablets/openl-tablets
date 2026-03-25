package org.openl.util.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlDBUtils {


    public static void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    public static void safeClose(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
            } catch (Exception e) {
                log.warn("Failed to commit", e);
            }
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    public static void safeClose(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }
}
