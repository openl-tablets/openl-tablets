package org.openl.studio.openapi.app000;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import org.openl.studio.openapi.AbstractStudioOpenApiTest;
import org.openl.studio.openapi.MockConfiguration;

@ContextConfiguration(classes = {MockConfiguration.class, TestRunner000.TestConfig.class})
public class TestRunner000 extends AbstractStudioOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {
    }
}
