package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class RmiJackrabbitDesignRepositoryFactory extends RmiJackrabbitRepositoryFactory {

    private ConfigPropertyString confRmiUrl = new ConfigPropertyString("design-repository.remote.rmi.url",
        "//localhost:1099/jackrabbit.repository");
    private final ConfigPropertyString login = new ConfigPropertyString("design-repository.login", null);
    private final ConfigPropertyString password = new ConfigPropertyString("design-repository.pass", null);

    public RmiJackrabbitDesignRepositoryFactory() {
        setUri(confRmiUrl);
        setLogin(login);
        setPassword(password);
    }
}
