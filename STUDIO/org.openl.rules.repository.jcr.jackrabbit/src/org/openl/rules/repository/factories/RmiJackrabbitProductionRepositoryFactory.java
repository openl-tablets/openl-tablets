package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class RmiJackrabbitProductionRepositoryFactory extends RmiJackrabbitRepositoryFactory {

    private ConfigPropertyString confRmiUrl = new ConfigPropertyString("production-repository.remote.rmi.url",
        "//localhost:1099/jackrabbit.repository");

    public RmiJackrabbitProductionRepositoryFactory() {
        setUri(confRmiUrl);
        setProductionRepositoryMode(true);
    }
}
