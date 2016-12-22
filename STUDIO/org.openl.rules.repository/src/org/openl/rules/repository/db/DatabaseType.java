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
            if (databaseType.code.equals(lowerName)) {
                return databaseType;
            }
        }

        return DEFAULT;
    }

    private final String code;

    DatabaseType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
