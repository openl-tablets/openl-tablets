package org.openl.rules.webstudio.migration;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

/**
 * An OpenL Studio database of an installation that still keeps its project tags in the legacy tables.
 *
 * <p>The tables are the ones such an installation carries, foreign keys included, so a test sees the names the
 * migration reads and the order it has to drop them in. They are written out here instead of being taken from
 * the schema of the running product, which is not the database the migration is meant to meet.
 *
 * @author Yury Molchan
 */
final class LegacyTagsDatabase implements AutoCloseable {

    /** The tag tables an installation from before the tags moved into the projects carries. */
    private static final List<String> LEGACY_TABLES = List.of("""
            CREATE TABLE OpenL_Tag_Types (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                extensible BOOLEAN NOT NULL,
                nullable BOOLEAN NOT NULL,
                CONSTRAINT uni_OpenL_Tag_Types1 UNIQUE (name))""",
            """
            CREATE TABLE OpenL_Tags (
                id BIGINT PRIMARY KEY,
                tag_type_id BIGINT NOT NULL,
                name VARCHAR(255) NOT NULL,
                CONSTRAINT fk_OpenL_Tags1 FOREIGN KEY (tag_type_id) REFERENCES OpenL_Tag_Types(id),
                CONSTRAINT uni_OpenL_Tags1 UNIQUE (tag_type_id, name))""",
            """
            CREATE TABLE OpenL_Projects (
                id BIGINT PRIMARY KEY,
                repository_id VARCHAR(255) NOT NULL,
                project_path VARCHAR(1000) NOT NULL)""",
            """
            CREATE TABLE OpenL_Project_Tags (
                project_id BIGINT NOT NULL,
                tag_id BIGINT NOT NULL,
                PRIMARY KEY (project_id, tag_id),
                CONSTRAINT fk_OpenL_Project_Tags1 FOREIGN KEY (project_id) REFERENCES OpenL_Projects(id),
                CONSTRAINT fk_OpenL_Project_Tags2 FOREIGN KEY (tag_id) REFERENCES OpenL_Tags(id))""");

    private final JdbcDataSource dataSource = new JdbcDataSource();

    LegacyTagsDatabase() throws SQLException {
        this("");
    }

    /**
     * @param options extra H2 settings, such as the identifier case the database keeps unquoted names in
     */
    LegacyTagsDatabase(String options) throws SQLException {
        // The database outlives the connections the migration opens and is closed by this fixture instead.
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1" + options);
        for (var table : LEGACY_TABLES) {
            execute(table);
        }
    }

    DataSource dataSource() {
        return dataSource;
    }

    void addTagType(long id, String name) throws SQLException {
        execute("INSERT INTO OpenL_Tag_Types (id, name, extensible, nullable) VALUES (%d, '%s', TRUE, TRUE)"
                .formatted(id, name));
    }

    void addTag(long id, long typeId, String name) throws SQLException {
        execute("INSERT INTO OpenL_Tags (id, tag_type_id, name) VALUES (%d, %d, '%s')".formatted(id, typeId, name));
    }

    void addProject(long id, String repositoryId, String projectPath) throws SQLException {
        execute("INSERT INTO OpenL_Projects (id, repository_id, project_path) VALUES (%d, '%s', '%s')"
                .formatted(id, repositoryId, projectPath));
    }

    void tagProject(long projectId, long tagId) throws SQLException {
        execute("INSERT INTO OpenL_Project_Tags (project_id, tag_id) VALUES (%d, %d)".formatted(projectId, tagId));
    }

    /**
     * Records one project tagged {@code Environment=production}, which is enough to migrate.
     */
    void addTaggedProject(String repositoryId, String projectPath) throws SQLException {
        addTagType(1, "Environment");
        addTag(10, 1, "production");
        addProject(100, repositoryId, projectPath);
        tagProject(100, 10);
    }

    void execute(String sql) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    int count(String sql) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    boolean tableExists(String table) throws SQLException {
        try (var connection = dataSource.getConnection();
             var tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void close() throws SQLException {
        execute("SHUTDOWN");
    }
}
