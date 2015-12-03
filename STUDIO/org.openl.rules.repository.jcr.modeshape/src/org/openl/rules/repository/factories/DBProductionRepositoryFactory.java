package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class DBProductionRepositoryFactory extends DBRepositoryFactory {

    private ConfigPropertyString dbUrl = new ConfigPropertyString("production-repository.db.url",
        "jdbc:mysql://localhost/production-repository");
    private final ConfigPropertyString login = new ConfigPropertyString("production-repository.login", null);
    private final ConfigPropertyString password = new ConfigPropertyString("production-repository.password", null);

    public DBProductionRepositoryFactory() {
        setLogin(login);
        setPassword(password);
        setUri(dbUrl);
        setProductionRepositoryMode(true);
    }
}
