package org.openl.rules.repository.db;

public enum DatabaseType {
    DEFAULT(""),
    H2("h2"),
    MYSQL("mysql"),
    POSTGRESQL("postgresql"),
    ORACLE("oracle"),
    SQL_SERVER("microsoft_sql_server");

    public static DatabaseType fromString(String name) {
        String lowerName = name.toLowerCase();
        for (DatabaseType databaseType : values()) {
            if (databaseType.name.equals(lowerName)) {
                return databaseType;
            }
        }

        return DEFAULT;
    }

    private final String name;

    DatabaseType(String name) {
        this.name = name;
    }
}
