package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class DBDesignRepositoryFactory extends DBRepositoryFactory {

    private ConfigPropertyString dbUrl = new ConfigPropertyString("design-repository.db.url",
        "jdbc:mysql://localhost/design-repository");

    public DBDesignRepositoryFactory() {
        setUri(dbUrl);
        setProductionRepositoryMode(false);
    }
}
