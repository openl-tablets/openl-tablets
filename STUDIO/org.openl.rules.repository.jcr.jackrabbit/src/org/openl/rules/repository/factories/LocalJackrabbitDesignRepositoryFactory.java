package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class LocalJackrabbitDesignRepositoryFactory extends LocalJackrabbitRepositoryFactory {

    private final ConfigPropertyString confRepositoryName = new ConfigPropertyString("design-repository.name",
        "Local Jackrabbit");

    public LocalJackrabbitDesignRepositoryFactory() {
        setConfRepositoryName(confRepositoryName);
        setProductionRepositoryMode(false);
    }
}
