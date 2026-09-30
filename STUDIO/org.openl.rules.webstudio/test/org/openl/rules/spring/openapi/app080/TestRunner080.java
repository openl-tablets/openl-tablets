package org.openl.rules.spring.openapi.app080;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;

/**
 * Test runner for @ModelAttribute parameter parsing in OpenAPI generation.
 * <p>
 * This test verifies that:
 * <ul>
 *   <li>@ModelAttribute parameters are expanded into individual form fields</li>
 *   <li>Field-level @Schema annotations are properly processed</li>
 *   <li>Jakarta validation annotations are converted to OpenAPI constraints</li>
 *   <li>Complex nested types (List&lt;MetadataEntry&gt;) are properly resolved</li>
 *   <li>MultipartFile fields are correctly typed as binary/file uploads</li>
 * </ul>
 */
@ContextConfiguration(classes = {MockConfiguration.class, TestRunner080.TestConfig.class})
public class TestRunner080 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {
    }

}
