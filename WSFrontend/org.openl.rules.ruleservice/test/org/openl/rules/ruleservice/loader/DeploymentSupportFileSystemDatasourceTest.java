package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.conf.LastVersionProjectsServiceConfigurer;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "ruleservice.datasource.dir=test-resources/DeploymentSupportFileSystemDatasourceTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.datasource.filesystem.supportDeployments = true",
        "ruleservice.datasource.filesystem.supportVersion = true" })
@ContextConfiguration({ "classpath:openl-ruleservice-property-placeholder.xml",
        "classpath:openl-ruleservice-datasource-beans.xml",
        "classpath:openl-ruleservice-loader-beans.xml" })
public class DeploymentSupportFileSystemDatasourceTest {
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
        assertEquals(1, servicesToBeDeployed.size());
        ServiceDescription serviceDescription = servicesToBeDeployed.iterator().next();
        assertEquals("deployment1", serviceDescription.getDeployment().getName());
        assertEquals("0.0.2", serviceDescription.getDeployment().getVersion().getVersionName());
    }
}
