package org.openl.rules.ruleservice.conf;

import org.openl.rules.ruleservice.loader.DataSource;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public final class DatasourceConfigurerFactoryBean extends AbstractFactoryBean<DataSource> {

    private static final String FILE_SYSTEM_DATASOURCE_BEAN_NAME = "fileSystemDataSource";
    private static final String PRODUCTION_REPOSITORY_DATASOURCE_BEAN_NAME = "productionRepositoryDataSource";

    @Override
    protected DataSource createInstance() throws Exception {
        return (DataSource) getBeanFactory().getBean(PRODUCTION_REPOSITORY_DATASOURCE_BEAN_NAME);
    }

    @Override
    public Class<?> getObjectType() {
        return DataSource.class;
    }
}
