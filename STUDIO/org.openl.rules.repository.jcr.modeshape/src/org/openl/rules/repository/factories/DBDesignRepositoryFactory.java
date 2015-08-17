package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class DBDesignRepositoryFactory extends DBRepositoryFactory {

    private ConfigPropertyString dbUrl = new ConfigPropertyString("design-repository.db.url",
        "jdbc:mysql://localhost/design-repository");
    private final ConfigPropertyString login = new ConfigPropertyString("design-repository.login", "user");
    private final ConfigPropertyString password = new ConfigPropertyString("design-repository.pass", "pass");

    public DBDesignRepositoryFactory() {
        setLogin(login);
        setPassword(password);
        setUrl(dbUrl);
    }
}
