package org.openl.rules.db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.openl.util.CollectionUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBMigrationBean {
    private final Logger log = LoggerFactory.getLogger(DBMigrationBean.class);

    private DataSource dataSource;

    public void init() throws Exception {
        Connection connection = dataSource.getConnection();
        String databaseCode;
        boolean oldMigrationExists = false;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
            String tableName = "schema_version";
            String repoTable = metaData.storesUpperCaseIdentifiers() ? tableName.toUpperCase() : tableName;

            ResultSet rs = null;
            try {
                if ("oracle".equals(databaseCode)) {
                    rs = metaData.getTables(null, metaData.getUserName(), repoTable, new String[] { "TABLE" });
                } else {
                    rs = metaData.getTables(null, null, repoTable, new String[] { "TABLE" });
                }
                oldMigrationExists = rs.next();
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        log.warn("Unexpected sql failure", e);
                    }
                }
            }
        } finally {
            connection.close();
        }

        String[] locations = { "/db/flyway/common", "/db/flyway/" + databaseCode };

        TreeMap<String, String> placeholders = new TreeMap<String, String>();
        for (String location : locations) {
            fillQueries(placeholders, location + "/placeholders.properties");
        }
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setBaselineVersionAsString("0");
        flyway.setBaselineOnMigrate(true);
        flyway.setTable("openl_security_flyway");
        flyway.setPlaceholders(placeholders);

        flyway.setLocations(locations);
        flyway.migrate();

        if (oldMigrationExists) {
            migrateOldFlywayData();
        }
    }

    // TODO: Remove it after 5.19.3 and early will become unsupported.
    // FIXME: WARNING! This migration MUST BE executed after V2_Initial_data.sql, before another migration script.
    private void migrateOldFlywayData() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        try {
            // Clean new data
            connection.prepareStatement("delete from OpenL_Group2Group").execute();
            connection.prepareStatement("delete from OpenL_User2Group").execute();
            connection.prepareStatement("delete from OpenL_Group_Authorities").execute();
            connection.prepareStatement("delete from OpenL_Groups").execute();
            connection.prepareStatement("delete from OpenL_Users").execute();

            // Migrate old data
            connection.prepareStatement(
                    "insert into OpenL_Users (loginName, password, firstName, surname) " +
                            "select LoginName, Password, FirstName, Surname " +
                            "from OpenLUser"
            ).execute();
            connection.prepareStatement(
                    "insert into OpenL_Groups (groupName, description) " +
                            "select GroupName, Description " +
                            "from UserGroup"
            ).execute();
            connection.prepareStatement(
                    "insert into OpenL_Group2Group (groupID, includedGroupID) " +
                            "select g1.id, g2.id " +
                            "from OpenL_Groups g1, OpenL_Groups g2, UserGroup ug1, UserGroup ug2, Group2Group g2g " +
                            "where ug1.GroupName = g1.groupName " +
                            "AND ug2.GroupName = g2.groupName " +
                            "AND ug1.GroupID = g2g.GroupID " +
                            "AND ug2.GroupID = g2g.IncludedGroupID"
            ).execute();
            connection.prepareStatement(
                    "insert into OpenL_User2Group (loginName, groupID) " +
                            "select us.LoginName, g1.id " +
                            "from OpenLUser us, OpenL_Groups g1, User2Group u2g, UserGroup ug1 " +
                            "where us.UserID = u2g.UserID " +
                            "AND ug1.GroupID = u2g.GroupID " +
                            "AND ug1.GroupName = g1.groupName"
            ).execute();
            ResultSet rs = connection.prepareStatement(
                    "select g1.id, ug.UserPrivileges " +
                            "from OpenL_Groups g1, UserGroup ug " +
                            "where ug.GroupName = g1.groupName"
            ).executeQuery();
            while (rs.next()) {
                Long groupId = rs.getLong(1);
                String userPrivileges = rs.getString(2);
                String[] oldPrivileges = StringUtils.split(userPrivileges, ',');
                if (CollectionUtils.isNotEmpty(oldPrivileges)) {
                    HashSet<String> newPrivileges = new HashSet<String>();
                    for (String oldPrivilege : oldPrivileges) {
                        String newPrivilege = oldPrivilege.substring("PRIVILEGE_".length());
                        if ("ALL".equals(newPrivilege) || "ADMINISTRATE".equals(newPrivilege)) {
                            newPrivilege = "ADMIN";
                        }
                        newPrivileges.add(newPrivilege);
                    }
                    for(String authority : newPrivileges) {
                        PreparedStatement st = connection.prepareStatement("insert into OpenL_Group_Authorities (groupID, authority) values (?, ?)");
                        st.setLong(1, groupId);
                        st.setString(2, authority);
                        st.executeUpdate();
                    }
                }
            }
            // Delete old tables
            connection.prepareStatement("drop table schema_version").execute();
            connection.prepareStatement("drop table AccessControlEntry").execute();
            connection.prepareStatement("drop table Group2Group").execute();
            connection.prepareStatement("drop table User2Group").execute();
            connection.prepareStatement("drop table UserGroup").execute();
            connection.prepareStatement("drop table OpenLUser").execute();
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        URL resource = getClass().getResource(propertiesFileName);
        if (resource == null) {
            log.info("File '{}' is not found.", propertiesFileName);
            return;
        }
        log.info("Load properties from '{}'.", resource);
        InputStream is = resource.openStream();
        try {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                queries.put(key, properties.getProperty(key));
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
