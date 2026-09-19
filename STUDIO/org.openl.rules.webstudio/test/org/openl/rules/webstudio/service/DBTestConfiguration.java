package org.openl.rules.webstudio.service;

import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ImportResource("classpath:META-INF/standalone/spring/security-hibernate-beans.xml")
public class DBTestConfiguration {

    @Bean
    public SessionRegistry sessionRegistry() {
        return mock(SessionRegistry.class);
    }

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
                .build();
    }

    @Bean
    public BeanPostProcessor beanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource source && "openlDataSource".equals(beanName)) {
                    return wrapLoggedDataSource(source);
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecuritySchemaReset securitySchemaReset(DataSource dataSource) {
        return new SecuritySchemaReset(dataSource);
    }

}
