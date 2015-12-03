package org.openl.rules.repository.factories;

/**
 * @author PUdalau
 */
public class WebDavJackrabbitDesignRepositoryFactory extends WebDavJacrabbitRepositoryFactory {

    public WebDavJackrabbitDesignRepositoryFactory() {
        setProductionRepositoryMode(false, "remote.webdav.url");
    }
}
