package org.openl.rules.spring.openapi.app031;

import java.util.Collections;
import java.util.List;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { MockConfiguration.class, TestRunner031.TestConfig.class })
public class TestRunner031 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {

        @Bean("openLRestExceptionBasePackages")
        @Primary
        public List<String> openLRestExceptionBasePackages() {
            return Collections.singletonList("org.openl.rules.spring.openapi.app031.exception");
        }
    }

}
