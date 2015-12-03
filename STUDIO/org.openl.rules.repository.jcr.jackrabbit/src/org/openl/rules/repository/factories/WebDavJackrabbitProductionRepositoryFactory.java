package org.openl.rules.repository.factories;

/**
 * @author PUdalau
 */
public class WebDavJackrabbitProductionRepositoryFactory extends WebDavJacrabbitRepositoryFactory {

    public WebDavJackrabbitProductionRepositoryFactory() {
        setProductionRepositoryMode(true);
    }
}
