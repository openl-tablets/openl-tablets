package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

public class LocalJackrabbitDesignRepositoryFactory extends LocalJackrabbitRepositoryFactory {

    private final ConfigPropertyString confRepositoryHome = new ConfigPropertyString(
            "design-repository.local.home", "../local-repository");
    private final ConfigPropertyString confNodeTypeFile = new ConfigPropertyString(
            "design-repository.jcr.nodetypes", DEFAULT_NODETYPE_FILE);
    private final ConfigPropertyString confRepositoryName = new ConfigPropertyString(
            "design-repository.name", "Local Jackrabbit");

    public LocalJackrabbitDesignRepositoryFactory() {
        setConfRepositoryHome(confRepositoryHome);
        setConfNodeTypeFile(confNodeTypeFile);
        setConfRepositoryName(confRepositoryName);
    }

}
