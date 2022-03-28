package org.openl.rules.spring.openapi.app050;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { MockConfiguration.class, TestRunner050.TestConfig.class })
public class TestRunner050 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {

        @Bean
        public GlobalSecurityConfig globalSecurityConfig() {
            return new GlobalSecurityConfig();
        }
    }


    @SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
    public static class GlobalSecurityConfig {

    }

}
