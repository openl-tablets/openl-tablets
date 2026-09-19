package org.openl.rules.webstudio.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;

/**
 * Project tags that OpenL Studio kept in its database before 6.0.0.
 *
 * <p>The tags of a project live in its {@code tags.properties} file since 6.0.0. This class reads what the
 * database still holds, so the tags can be written to the projects, and drops the legacy tables once every
 * project has its own file.
 *
 * <p>The legacy tables are optional. An installation that no longer has them has nothing to migrate and nothing
 * to drop, so the tables themselves say whether there is anything left to do. A drop that stopped halfway
 * leaves the table it did not reach, which the next drop finishes.
 */
@RequiredArgsConstructor
final class LegacyProjectTags {

    private static final String PROJECTS_TABLE = "OPENL_PROJECTS";
    private static final String PROJECT_TAGS_TABLE = "OPENL_PROJECT_TAGS";
    private static final Set<String> READABLE_TABLES = Set
            .of(PROJECTS_TABLE, PROJECT_TAGS_TABLE, "OPENL_TAGS", "OPENL_TAG_TYPES");
    private static final String[] TABLE_TYPE = {"TABLE"};

    private static final String SELECT_TAGGED_PROJECTS = """
            SELECT p.id, p.repository_id, p.project_path, tt.name, t.name \
            FROM OpenL_Projects p \
            JOIN OpenL_Project_Tags pt ON pt.project_id = p.id \
            JOIN OpenL_Tags t ON t.id = pt.tag_id \
            JOIN OpenL_Tag_Types tt ON tt.id = t.tag_type_id""";

    private static final String DROP_PROJECT_TAGS = "DROP TABLE OpenL_Project_Tags";
    private static final String DROP_PROJECTS = "DROP TABLE OpenL_Projects";

    private final DataSource dataSource;

    /**
     * A project of the legacy tables and the tags recorded for it.
     *
     * @param repositoryId the repository the project belongs to
     * @param projectPath  the path of the project in its repository
     * @param tags         the tags as {@code tag type -> tag value}
     */
    record Project(String repositoryId, String projectPath, Map<String, String> tags) {
    }

    /**
     * Tells whether the database still carries a legacy tag table.
     *
     * <p>One table alone is enough: a drop that stopped halfway leaves work to finish.
     */
    boolean exists() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            var tables = tableNames(connection);
            return tables.contains(PROJECTS_TABLE) || tables.contains(PROJECT_TAGS_TABLE);
        }
    }

    /**
     * Reads every project that still has tags in the database.
     *
     * <p>A project without tags is not returned: it has nothing to write to its {@code tags.properties} file.
     *
     * @return the tagged projects, or an empty list when a table the tags are read from is absent
     */
    List<Project> read() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            if (!tableNames(connection).containsAll(READABLE_TABLES)) {
                return List.of();
            }
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery(SELECT_TAGGED_PROJECTS)) {
                return collect(rows);
            }
        }
    }

    /**
     * Drops the legacy tables the database still carries, which retires them once every project has its tags in
     * its own file.
     *
     * <p>The tables the tags are shared with, {@code OpenL_Tags} and {@code OpenL_Tag_Types}, are still in use
     * and are kept.
     */
    void drop() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            var tables = tableNames(connection);
            // The link table carries the foreign key, so it goes first.
            dropPresent(statement, tables, PROJECT_TAGS_TABLE, DROP_PROJECT_TAGS);
            dropPresent(statement, tables, PROJECTS_TABLE, DROP_PROJECTS);
        }
    }

    private static void dropPresent(Statement statement,
                                    Set<String> tables,
                                    String table,
                                    String drop) throws SQLException {
        if (tables.contains(table)) {
            statement.execute(drop);
        }
    }

    private static List<Project> collect(ResultSet rows) throws SQLException {
        var projects = new LinkedHashMap<Long, Project>();
        while (rows.next()) {
            var id = rows.getLong(1);
            var repositoryId = rows.getString(2);
            var projectPath = rows.getString(3);
            var tagType = rows.getString(4);
            var tagValue = rows.getString(5);
            projects.computeIfAbsent(id, key -> new Project(repositoryId, projectPath, new LinkedHashMap<>()))
                    .tags()
                    .put(tagType, tagValue);
        }
        return List.copyOf(projects.values());
    }

    /**
     * The tables of the schema the connection works with, in upper case.
     *
     * <p>A database keeps an unquoted name in its own case, so the names are compared without it.
     */
    private static Set<String> tableNames(Connection connection) throws SQLException {
        var names = new HashSet<String>();
        try (var tables = connection.getMetaData()
                .getTables(connection.getCatalog(), connection.getSchema(), null, TABLE_TYPE)) {
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME").toUpperCase(Locale.ROOT));
            }
        }
        return names;
    }
}
