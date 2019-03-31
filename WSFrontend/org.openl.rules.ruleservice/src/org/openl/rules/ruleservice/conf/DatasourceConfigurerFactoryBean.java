package org.openl.rules.ruleservice.conf;

import org.openl.rules.ruleservice.loader.DataSource;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public final class DatasourceConfigurerFactoryBean extends AbstractFactoryBean<DataSource> {

    private static final String FILE_SYSTEM_DATASOURCE_BEAN_NAME = "fileSystemDataSource";
    private static final String PRODUCTION_REPOSITORY_DATASOURCE_BEAN_NAME = "productionRepositoryDataSource";

    private boolean fileSystemDatasource = true;

    public boolean isFileSystemDatasource() {
        return fileSystemDatasource;
    }

    public void setFileSystemDatasource(boolean fileSystemDatasource) {
        this.fileSystemDatasource = fileSystemDatasource;
    }

    @Override
    protected DataSource createInstance() throws Exception {
        if (isFileSystemDatasource()) {
            return (DataSource) getBeanFactory().getBean(FILE_SYSTEM_DATASOURCE_BEAN_NAME);
        }
        return (DataSource) getBeanFactory().getBean(PRODUCTION_REPOSITORY_DATASOURCE_BEAN_NAME);
    }

    @Override
    public Class<?> getObjectType() {
        return DataSource.class;
    }
}
