package org.openl.rules.ruleservice.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
public class LastVersionProjectsServiceConfigurerTest {
    private static final String PROJECT_NAME = "openl-project";

    @Autowired
    private RuleServiceLoader rulesLoader;

    public RuleServiceLoader getRulesLoader() {
        return rulesLoader;
    }

    public void setRulesLoader(RuleServiceLoader rulesLoader) {
        this.rulesLoader = rulesLoader;
    }

    @Test
    public void testConfigurer() {
        LastVersionProjectsServiceConfigurer configurer = new LastVersionProjectsServiceConfigurer();
        Collection<ServiceDescription> servicesToBeDeployed = configurer.getServicesToBeDeployed(rulesLoader);
        assertEquals(2, servicesToBeDeployed.size());
        Set<String> serviceNames = new HashSet<>();
        for (ServiceDescription description : servicesToBeDeployed) {
            serviceNames.add(description.getName());
        }
        assertTrue(serviceNames.contains(PROJECT_NAME));
    }

    @Test
    public void shouldConfigureDeployments_whenDeploymentMatcherIsSet() {
        LastVersionProjectsServiceConfigurer configurer = new LastVersionProjectsServiceConfigurer();
        configurer.setDatasourceDeploymentPatterns("*Projects*");
        Collection<ServiceDescription> servicesToBeDeployed = configurer.getServicesToBeDeployed(rulesLoader);
        assertEquals(2, servicesToBeDeployed.size());
        Set<String> serviceNames = new HashSet<>();
        for (ServiceDescription description : servicesToBeDeployed) {
            serviceNames.add(description.getName());
        }
        assertTrue(serviceNames.contains(PROJECT_NAME));
    }

    @Test
    public void shouldNotMatchAnyDeployments_whenDeploymentMatcherIsSet() {
        LastVersionProjectsServiceConfigurer configurer = new LastVersionProjectsServiceConfigurer();
        configurer.setDatasourceDeploymentPatterns("Test*");
        Collection<ServiceDescription> servicesToBeDeployed = configurer.getServicesToBeDeployed(rulesLoader);
        assertEquals(0, servicesToBeDeployed.size());
    }

}
