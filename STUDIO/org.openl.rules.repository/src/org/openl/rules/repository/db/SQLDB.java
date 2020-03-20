package org.openl.rules.repository.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SqlDB {

    private final Logger log = LoggerFactory.getLogger(SqlDB.class);

    protected void safeClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

    protected void safeClose(Connection connection) {
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

    protected void safeClose(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (Exception e) {
                log.warn("Unexpected sql failure", e);
            }
        }
    }

}
