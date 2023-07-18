package org.openl.rules.ruleservice.spring;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * The application configuration for beans which have difficulties for configuring from the XML.
 *
 * @author Yury Molchn
 */
@Configuration
public class AppConfiguration {

    @Lazy
    @Bean()
    Object initOpenAPIDefaultContext() throws OpenApiConfigurationException {
        // 'parent' configuration for all customization of OpenAPI configuration
        return new JaxrsOpenApiContextBuilder<>().configLocation("openapi-configuration-default.json").buildContext(true);
    }

}
