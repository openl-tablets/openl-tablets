package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

/**
 * @author PUdalau
 */
public class WebDavJackrabbitProductionRepositoryFactory extends WebDavJacrabbitRepositoryFactory {

    private ConfigPropertyString confWebdavUrl = new ConfigPropertyString("production-repository.remote.webdav.url",
        "http://localhost:8080/jcr/server/");

    public WebDavJackrabbitProductionRepositoryFactory() {
        setUri(confWebdavUrl);
        setProductionRepositoryMode(true);
    }
}
