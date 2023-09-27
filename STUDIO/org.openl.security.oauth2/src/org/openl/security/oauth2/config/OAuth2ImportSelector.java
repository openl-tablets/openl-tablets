package org.openl.security.oauth2.config;

import org.openl.util.StringUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Selects OAuth2 configuration based on the presence of the introspection-uri property.
 */
public class OAuth2ImportSelector implements ImportSelector, EnvironmentAware {

    private Environment environment;

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        var introspectionUri = environment.getProperty("security.oauth2.introspection-uri");
        if (StringUtils.isNotBlank(introspectionUri)) {
            return new String[] { OAuth2OpaqueAccessTokenConfiguration.class.getName() };
        } else {
            return new String[] { OAuth2JwtAccessTokenConfiguration.class.getName() };
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
