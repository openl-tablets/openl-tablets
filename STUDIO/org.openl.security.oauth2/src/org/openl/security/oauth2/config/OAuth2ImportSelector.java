package org.openl.security.oauth2.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import org.openl.security.oauth2.OAuth2Configuration;

public class OAuth2ImportSelector implements ImportSelector, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    @NonNull
    public String[] selectImports(@NonNull AnnotationMetadata metadata) {
        var oAuth2Config = beanFactory.getBean(OAuth2Configuration.class);

        if (oAuth2Config.getIntrospectionEndpoint().isPresent()) {
            return new String[] { OAuth2OpaqueAccessTokenConfiguration.class.getName() };
        } else {
            return new String[] { OAuth2JwtAccessTokenConfiguration.class.getName() };
        }
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
}
