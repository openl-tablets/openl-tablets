package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class DBProductionRepositoryFactory extends DBRepositoryFactory {

    private ConfigPropertyString dbUrl = new ConfigPropertyString("production-repository.db.url",
        "jdbc:mysql://localhost/production-repository");

    public DBProductionRepositoryFactory() {
        setUri(dbUrl);
        setProductionRepositoryMode(true);
    }
}
