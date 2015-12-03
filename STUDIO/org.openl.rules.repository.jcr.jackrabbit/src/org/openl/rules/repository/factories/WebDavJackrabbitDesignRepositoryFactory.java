package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

/**
 * @author PUdalau
 */
public class WebDavJackrabbitDesignRepositoryFactory extends WebDavJacrabbitRepositoryFactory {

    private ConfigPropertyString confWebdavUrl = new ConfigPropertyString("design-repository.remote.webdav.url",
        "http://localhost:8080/jcr/server/");

    public WebDavJackrabbitDesignRepositoryFactory() {
        setUri(confWebdavUrl);
        setProductionRepositoryMode(false);
    }
}
