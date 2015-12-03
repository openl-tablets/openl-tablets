package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class LocalJackrabbitDesignRepositoryFactory extends LocalJackrabbitRepositoryFactory {

    private final ConfigPropertyString confRepositoryHome = new ConfigPropertyString("design-repository.local.home",
        "../local-repository");
    private final ConfigPropertyString confRepositoryName = new ConfigPropertyString("design-repository.name",
        "Local Jackrabbit");
    private final ConfigPropertyString login = new ConfigPropertyString("design-repository.login", null);
    private final ConfigPropertyString password = new ConfigPropertyString("design-repository.pass", null);

    public LocalJackrabbitDesignRepositoryFactory() {
        setUri(confRepositoryHome);
        setConfRepositoryName(confRepositoryName);
        setLogin(login);
        setPassword(password);
    }
}
