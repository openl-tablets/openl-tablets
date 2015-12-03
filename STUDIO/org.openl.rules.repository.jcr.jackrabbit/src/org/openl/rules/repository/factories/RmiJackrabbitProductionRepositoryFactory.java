package org.openl.rules.repository.factories;

public class RmiJackrabbitProductionRepositoryFactory extends RmiJackrabbitRepositoryFactory {

    public RmiJackrabbitProductionRepositoryFactory() {
        setProductionRepositoryMode(true, "remote.rmi.url");
    }
}
