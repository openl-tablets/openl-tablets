package org.openl.rules.ruleservice.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

@TestPropertySource(properties = {"production-repository.uri=test-resources/LastVersionProjectsServiceConfigurerTest",
        "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-property-placeholder.xml",
        "classpath:openl-ruleservice-datasource-beans.xml"})
class LastVersionProjectsServiceConfigurerTest {
    private static final String PROJECT_NAME = "openl-project";

    @Autowired
    private RuleServiceLoader rulesLoader;

    @Test
    void testConfigurer() {
        var configurer = new LastVersionProjectsServiceConfigurer();
        var servicesToBeDeployed = configurer.getServicesToBeDeployed(rulesLoader);
        assertEquals(2, servicesToBeDeployed.size());
        var serviceNames = new HashSet<String>();
        for (ServiceDescription description : servicesToBeDeployed) {
            serviceNames.add(description.getName());
        }
        assertTrue(serviceNames.contains(PROJECT_NAME));
    }

    @Test
    void shouldConfigureDeployments_whenDeploymentMatcherIsSet() {
        var configurer = new LastVersionProjectsServiceConfigurer();
        configurer.setDatasourceDeploymentPatterns("*Projects*");
        var servicesToBeDeployed = configurer.getServicesToBeDeployed(rulesLoader);
        assertEquals(2, servicesToBeDeployed.size());
        var serviceNames = new HashSet<String>();
        for (ServiceDescription description : servicesToBeDeployed) {
            serviceNames.add(description.getName());
        }
        assertTrue(serviceNames.contains(PROJECT_NAME));
    }

    @Test
    void shouldNotMatchAnyDeployments_whenDeploymentMatcherIsSet() {
        var configurer = new LastVersionProjectsServiceConfigurer();
        configurer.setDatasourceDeploymentPatterns("Test*");
        var servicesToBeDeployed = configurer.getServicesToBeDeployed(rulesLoader);
        assertEquals(0, servicesToBeDeployed.size());
    }

}
