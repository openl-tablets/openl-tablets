package org.openl.rules.webstudio.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.openl.util.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Configuration
@ImportResource("classpath:META-INF/standalone/spring/security-hibernate-beans.xml")
@ComponentScan(basePackages = "org.openl.rules.webstudio.service")
public class DBTestConfiguration {

    /**
     * Wraps original datasource with proxy DataSource. This proxy helps to analyze generated SQL queries
     * 
     * @param dataSource original bean
     * @return proxied bean
     */
    private DataSource wrapLoggedDataSource(DataSource dataSource) {
        return ProxyDataSourceBuilder.create(dataSource)
            .name("OpenL-DataSource-Logger")
            .asJson()
            .countQuery()
            .logQueryToSysOut()
            .build();
    }

    @Bean
    public BeanPostProcessor beanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource && "openlDataSource".equals(beanName)) {
                    return wrapLoggedDataSource((DataSource) bean);
                }
                return bean;
            }
        };
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public Flyway flywayDBReset(DataSource dataSource) throws SQLException, IOException {
        String databaseCode;
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
        }

        String[] locations = { "/db/flyway/common", "/db/flyway/" + databaseCode };

        TreeMap<String, String> placeholders = new TreeMap<>();
        for (String location : locations) {
            fillQueries(placeholders, location + "/placeholders.properties");
        }
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setBaselineVersionAsString("0");
        flyway.setBaselineOnMigrate(true);
        flyway.setTable("openl_security_flyway");
        flyway.setPlaceholders(placeholders);

        flyway.setLocations(locations);

        return flyway;
    }

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        URL resource = getClass().getResource(propertiesFileName);
        if (resource == null) {
            return;
        }
        InputStream is = resource.openStream();
        try {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                queries.put(key, properties.getProperty(key));
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
