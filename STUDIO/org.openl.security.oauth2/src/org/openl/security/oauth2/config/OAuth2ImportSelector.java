package org.openl.security.oauth2.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import org.openl.security.oauth2.OAuth2Configuration;

public class OAuth2ImportSelector implements ImportSelector, BeanFactoryAware, EnvironmentAware {

    private DefaultListableBeanFactory beanFactory;
    private Environment environment;

    @Override
    @NonNull
    public String[] selectImports(@NonNull AnnotationMetadata metadata) {
        var issuerUri = environment.getProperty("security.oauth2.issuer-uri");
        var oauth2Config = new OAuth2Configuration(issuerUri);
        beanFactory.registerSingleton("oauth2Config", oauth2Config);

        if (oauth2Config.getIntrospectionEndpoint().isPresent()) {
            return new String[]{OAuth2OpaqueAccessTokenConfiguration.class.getName()};
        } else {
            return new String[]{OAuth2JwtAccessTokenConfiguration.class.getName()};
        }
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }
}
