package org.openl.rules.repository.factories;

public class DBProductionRepositoryFactory extends DBRepositoryFactory {

    public DBProductionRepositoryFactory() {
        setProductionRepositoryMode(true, "db.url");
    }
}
