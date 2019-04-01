package org.openl.rules.ruleservice.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.datasource.dir=test-resources/LastVersionProjectsServiceConfigurerTest" })
@ContextConfiguration({ "classpath:openl-ruleservice-property-placeholder.xml",
        "classpath:openl-ruleservice-datasource-beans.xml",
        "classpath:openl-ruleservice-loader-beans.xml" })
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
        assertEquals(serviceNames.size(), servicesToBeDeployed.size());
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
        assertEquals(serviceNames.size(), servicesToBeDeployed.size());
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
