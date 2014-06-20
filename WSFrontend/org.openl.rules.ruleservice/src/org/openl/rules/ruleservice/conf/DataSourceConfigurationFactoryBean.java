package org.openl.rules.ruleservice.conf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.loader.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DataSourceConfigurationFactoryBean implements FactoryBean<DataSource>, ApplicationContextAware, InitializingBean {

    private final Log log = LogFactory.getLog(DataSourceConfigurationFactoryBean.class);

    private static final String JCR_DATASOURCE_BEAN_NAME = "jcrdatasource";
    private static final String FILESYSTEM_DATASOURCE_BEAN_NAME = "filesystemdatasource";

    private static final String JCR_DATASOURCE = "jcr";
    private static final String FILESYSTEM_DATASOURCE = "local";

    private ApplicationContext applicationContext;

    private String datasourceType = JCR_DATASOURCE;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public DataSource getObject() throws Exception {
        if (JCR_DATASOURCE.equalsIgnoreCase(getDatasourceType())) {
            log.info("JCR Datasource is enabled!");
            return applicationContext.getBean(JCR_DATASOURCE_BEAN_NAME, DataSource.class);
        } else {
            log.info("File System Datasource is enabled!");
            return applicationContext.getBean(FILESYSTEM_DATASOURCE_BEAN_NAME, DataSource.class);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return DataSource.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!(JCR_DATASOURCE.equalsIgnoreCase(getDatasourceType()) || FILESYSTEM_DATASOURCE.equalsIgnoreCase(getDatasourceType()))) {
            throw new IllegalArgumentException("Property 'datasourceType' is required! Supported values is '" + JCR_DATASOURCE + "' or '" + FILESYSTEM_DATASOURCE + "'!");
        }
    }

}
