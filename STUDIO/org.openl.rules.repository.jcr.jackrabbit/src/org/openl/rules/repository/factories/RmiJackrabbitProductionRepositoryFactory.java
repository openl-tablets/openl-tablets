package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class RmiJackrabbitProductionRepositoryFactory extends RmiJackrabbitRepositoryFactory {

    private ConfigPropertyString confRmiUrl = new ConfigPropertyString("production-repository.remote.rmi.url",
        "//localhost:1099/jackrabbit.repository");
    private final ConfigPropertyString login = new ConfigPropertyString("production-repository.login", "user");
    private final ConfigPropertyString password = new ConfigPropertyString("production-repository.password", "pass");
    private final ConfigPropertyString repoConfigFile = new ConfigPropertyString("production-repository.config",
        "/jackrabbit-repository.xml");

    public RmiJackrabbitProductionRepositoryFactory() {
        setUri(confRmiUrl);
        setLogin(login);
        setPassword(password);
        setRepoConfigFile(repoConfigFile);
        setProductionRepositoryMode(true);
    }
}
