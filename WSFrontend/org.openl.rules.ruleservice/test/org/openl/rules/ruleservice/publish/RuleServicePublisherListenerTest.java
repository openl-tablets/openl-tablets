package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.servlet.ServiceInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "production-repository.uri=test-resources/RulesPublisherTest",
        "ruleservice.isProvideRuntimeContext=false",
        "production-repository.factory = repo-file"})
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml", "classpath:RuleServicePublisherListenerTest.xml" })
public class RuleServicePublisherListenerTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void test() throws Exception {
        assertNotNull(applicationContext);
        ServiceInfoProvider serviceManager = applicationContext.getBean("serviceManager", ServiceInfoProvider.class);
        assertNotNull(serviceManager);
        ServiceManager publisher = applicationContext.getBean("serviceManager", ServiceManager.class);

        Assert.assertFalse(serviceManager.getServicesInfo().isEmpty());

        OpenLService service = publisher.getServiceByDeploy("org.openl.tablets.tutorial4");

        Assert.assertEquals(2, RuleServicePublisherListenerTestListener.onDeployCount);
        Assert.assertEquals(0, RuleServicePublisherListenerTestListener.onUndeployCount);

        publisher.undeploy("org.openl.tablets.tutorial4");
        publisher.deploy(service);

        Assert.assertEquals(3, RuleServicePublisherListenerTestListener.onDeployCount);
        Assert.assertEquals(1, RuleServicePublisherListenerTestListener.onUndeployCount);

        publisher.undeploy("org.openl.tablets.tutorial4");

        Assert.assertEquals(3, RuleServicePublisherListenerTestListener.onDeployCount);
        Assert.assertEquals(2, RuleServicePublisherListenerTestListener.onUndeployCount);

    }
}
